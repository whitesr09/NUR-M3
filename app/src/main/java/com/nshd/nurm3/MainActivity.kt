package com.nshd.nurm3

import android.hardware.biometrics.BiometricPrompt
import android.hardware.biometrics.BiometricManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.nshd.nurm3.data.*
import com.nshd.nurm3.focus.FocusController
import com.nshd.nurm3.ui.*
import com.nshd.nurm3.widget.NurWidgetProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    private val model: NurViewModel by viewModels()
    private val focus: FocusController by viewModels()
    private val privateStore by lazy { NurPrivateStore(this) }
    private val lock by lazy { NurLock(privateStore) }
    val refreshController by lazy { NurRefreshController(window, this) }
    private val requestedRoute = MutableStateFlow("")
    private var authResult: ((Boolean, String) -> Unit)? = null

    override fun attachBaseContext(newBase: Context) { super.attachBaseContext(NurAccessibilityStore.localized(newBase)) }

    private val credentialLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val callback = authResult
        authResult = null
        callback?.invoke(result.resultCode == RESULT_OK, if (result.resultCode == RESULT_OK) "" else "Device authentication was cancelled")
    }

    private fun authenticateDevice(callback: (Boolean, String) -> Unit) {
        if (authResult != null) { callback(false, "Authentication is already in progress"); return }
        val manager = getSystemService(KeyguardManager::class.java)
        if (!manager.isDeviceSecure) { callback(false, "Set a secure Android screen lock first"); return }
        authResult = callback
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                val prompt = BiometricPrompt.Builder(this)
                    .setTitle("Unlock NUR")
                    .setSubtitle("Confirm your identity")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()
                prompt.authenticate(CancellationSignal(), mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        val next = authResult; authResult = null; next?.invoke(true, "")
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        val next = authResult; authResult = null; next?.invoke(false, errString.toString())
                    }
                })
            } catch (error: Exception) {
                val next = authResult; authResult = null; next?.invoke(false, error.message ?: "Authentication unavailable")
            }
        } else {
            val intent = manager.createConfirmDeviceCredentialIntent("Unlock NUR", "Confirm your screen lock")
            if (intent != null) credentialLauncher.launch(intent)
            else { authResult = null; callback(false, "Device credential unavailable") }
        }
    }

    private fun acceptRoute(intent: Intent?) {
        val route = intent?.getStringExtra(NurWidgetProvider.EXTRA_ROUTE) ?: return
        if (route in setOf("journey", "amanah", "amanah?create=true", "dhikr", "focus", "reflections", "settings", "ai")) requestedRoute.value = route
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        lifecycle.addObserver(refreshController)
        acceptRoute(intent)
        setContent {
            val route by requestedRoute.collectAsStateWithLifecycle()
            NurApp(model, focus, lock, ::authenticateDevice, route) { requestedRoute.value = "" }
        }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); acceptRoute(intent) }
    override fun onResume() { super.onResume(); model.refreshDate(); NurWidgetProvider.refresh(this) }
    override fun onStop() { focus.pause(); lock.lock(); super.onStop() }
    override fun onDestroy() { authResult = null; super.onDestroy() }
}

@Composable
fun NurApp(
    model: NurViewModel,
    focus: FocusController,
    lock: NurLock,
    authenticate: ((Boolean, String) -> Unit) -> Unit,
    requestedRoute: String = "",
    consumeRoute: () -> Unit = {}
) {
    val savedPrefs by model.preferences.collectAsStateWithLifecycle()
    val entries by model.entries.collectAsStateWithLifecycle()
    val allEntries by model.allEntries.collectAsStateWithLifecycle()
    val completions by model.completions.collectAsStateWithLifecycle()
    val today by model.today.collectAsStateWithLifecycle()
    val locked by lock.locked.collectAsStateWithLifecycle()
    val activity = androidx.compose.ui.platform.LocalContext.current as MainActivity
    val accessStore = remember(activity) { NurAccessibilityStore.get(activity) }
    val accessibility by accessStore.settings.collectAsStateWithLifecycle()
    val prefs = savedPrefs.copy(
        typeScale = (savedPrefs.typeScale * accessibility.textScale).coerceIn(0.85f, 2f),
        reduceMotion = savedPrefs.reduceMotion || accessibility.reduceMotion || !android.animation.ValueAnimator.areAnimatorsEnabled(),
        compactCards = savedPrefs.compactCards && accessibility.textScale <= 1.1f
    )
    val refreshStatus by activity.refreshController.status.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val current = (nav.currentBackStackEntryAsState().value?.destination?.route ?: "journey").substringBefore("?")
    val snackbar = remember { SnackbarHostState() }
    var showGuide by rememberSaveable {
        mutableStateOf(!activity.getSharedPreferences("nur_onboarding", Context.MODE_PRIVATE).getBoolean("guide_seen", false))
    }
    val dark = when (prefs.themeMode) {
        "light" -> false
        "system" -> isSystemInDarkTheme()
        else -> true
    }
    val tabs = remember(prefs.bottomNavigation) { resolveNurTabs(prefs.bottomNavigation) }
    val tabRoutes = remember(tabs) { tabs.map { it.route }.toSet() }
    val showBottomBar = current in tabRoutes

    LaunchedEffect(prefs.highRefreshRate, locked) {
        activity.refreshController.setEnabled(prefs.highRefreshRate && !locked)
    }
    SideEffect {
        if (locked || prefs.privatePreview || current in setOf("ai", "secure-backup")) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            model.refreshDate()
            delay(30_000)
        }
    }
    LaunchedEffect(model, locked) {
        if (!locked) model.uiEvents.collectLatest { snackbar.showSnackbar(it) }
    }
    LaunchedEffect(entries, completions, today, prefs.widgetsEnabled) {
        NurWidgetProvider.refresh(activity)
    }

    NurAccessibleTheme(prefs, accessibility) {
        CompositionLocalProvider(LocalNurSnackbarHost provides snackbar) {
            if (locked) {
                LockScreen(lock, authenticate)
            } else {
                val navigate: (String) -> Unit = { route ->
                    if (route != current || route.contains('?')) {
                        if (route.substringBefore("?") in tabRoutes) {
                            nav.navigate(route) {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else {
                            nav.navigate(route) { launchSingleTop = true }
                        }
                    }
                }

                LaunchedEffect(requestedRoute, locked) {
                    if (!locked && requestedRoute.isNotBlank()) {
                        navigate(requestedRoute)
                        consumeRoute()
                    }
                }

                val enter = if (prefs.reduceMotion) EnterTransition.None else fadeIn(tween(220))
                val exit = if (prefs.reduceMotion) ExitTransition.None else fadeOut(tween(120))

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ) {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        topBar = {
                            NurTopBar(
                                current,
                                today,
                                tabs = tabs,
                                onBack = { if (!nav.popBackStack()) navigate("journey") },
                                onSettings = { navigate("settings") }
                            )
                        },
                        bottomBar = {
                            if (showBottomBar) NurBottomBar(current, tabs, navigate)
                        },
                        floatingActionButton = {
                            if (current != "ai") {
                                NurGlassFab(
                                    onClick = { navigate("ai") },
                                    contentDescription = "Open NUR AI"
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = "Open NUR AI",
                                        modifier = Modifier.size(27.dp)
                                    )
                                }
                            }
                        },
                        floatingActionButtonPosition = FabPosition.End,
                        snackbarHost = { SnackbarHost(snackbar) }
                    ) { padding ->
                        NavHost(
                            navController = nav,
                            startDestination = "journey",
                            modifier = Modifier.fillMaxSize().padding(padding),
                            enterTransition = { enter },
                            exitTransition = { exit },
                            popEnterTransition = { enter },
                            popExitTransition = { exit }
                        ) {
                            composable("journey") {
                                DailyJourneyScreen14(entries, completions, today, prefs, model, navigate)
                            }
                            composable(
                                "amanah?create={create}",
                                arguments = listOf(
                                    androidx.navigation.navArgument("create") {
                                        type = androidx.navigation.NavType.BoolType
                                        defaultValue = false
                                    }
                                )
                            ) { entry ->
                                EntryScreen(
                                    "Amanah",
                                    "Your daily responsibilities",
                                    NurKind.AMANAH,
                                    entries,
                                    completions,
                                    today,
                                    model,
                                    initialAdd = entry.arguments?.getBoolean("create") == true
                                )
                            }
                            composable("muhasaba") {
                                EntryScreen("Muhasaba", "Reflect", NurKind.MUHASABA, entries, completions, today, model)
                            }
                            composable("rhythm") {
                                EntryScreen("Rhythm", "Build consistent habits", NurKind.RHYTHM, entries, completions, today, model)
                            }
                            composable("history") { HistoryScreen(allEntries, completions) }
                            composable("settings") { PowerSettingsScreen(prefs, model, navigate, lock) }
                            composable("appearance") { AppearanceStudio27(prefs, model, refreshStatus, navigate) }
                            composable("fonts") { NurFontSettingsScreen(prefs, model) }
                            composable("navigation") { NavigationSettingsScreen(prefs, model) }
                            composable("layout") { JourneyStudio14(prefs, model) }
                            composable("insights") { InsightsScreen(allEntries, completions, today) }
                            composable("backup") { BackupScreen() }
                            composable("secure-backup") { SecureBackupScreen() }
                            composable("backup-health") { BackupHealthScreen() }
                            composable("privacy") {
                                PrivacyScreen(lock, prefs.privatePreview) { model.setting("private_preview", it) }
                            }
                            composable("dhikr") { DhikrScreen(model, prefs, today) }
                            composable("focus") { FocusScreen(focus) }
                            composable("ai") { NurAiScreen(prefs, model) }
                            composable("widgets") { NurWidgetSettingsScreen(prefs, model) }
                            composable("accessibility") { NurAccessibilityScreen(prefs, model) }
                            composable(
                                "reflections?verse={verse}",
                                arguments = listOf(
                                    androidx.navigation.navArgument("verse") {
                                        type = androidx.navigation.NavType.StringType
                                        defaultValue = ""
                                    }
                                )
                            ) { entry ->
                                ReflectionScreen(entry.arguments?.getString("verse"))
                            }
                        }
                    }
                }

                if (showGuide && requestedRoute.isBlank()) {
                    NurFeatureGuide(onDismiss = {
                        activity.getSharedPreferences("nur_onboarding", Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("guide_seen", true)
                            .apply()
                        showGuide = false
                    })
                }
            }
        }
    }
}
