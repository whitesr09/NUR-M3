package com.nshd.nurm3

import android.hardware.biometrics.BiometricPrompt
import android.hardware.biometrics.BiometricManager
import android.app.KeyguardManager
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.nshd.nurm3.data.*
import com.nshd.nurm3.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    private val model: NurViewModel by viewModels()
    private val privateStore by lazy { NurPrivateStore(this) }
    private val lock by lazy { NurLock(privateStore) }
    val refreshController by lazy { NurRefreshController(window, this) }
    private var authResult: ((Boolean, String) -> Unit)? = null
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        lifecycle.addObserver(refreshController)
        setContent { NurApp(model, lock, ::authenticateDevice) }
    }
    override fun onResume() { super.onResume(); model.refreshDate() }
    override fun onStop() { lock.lock(); super.onStop() }
    override fun onDestroy() { authResult = null; super.onDestroy() }
}

@Composable
fun NurApp(model: NurViewModel, lock: NurLock, authenticate: ((Boolean, String) -> Unit) -> Unit) {
    val prefs by model.preferences.collectAsStateWithLifecycle()
    val entries by model.entries.collectAsStateWithLifecycle()
    val allEntries by model.allEntries.collectAsStateWithLifecycle()
    val completions by model.completions.collectAsStateWithLifecycle()
    val today by model.today.collectAsStateWithLifecycle()
    val locked by lock.locked.collectAsStateWithLifecycle()
    val activity = androidx.compose.ui.platform.LocalContext.current as MainActivity
    val refreshStatus by activity.refreshController.status.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val current = nav.currentBackStackEntryAsState().value?.destination?.route ?: "journey"
    val snackbar = remember { SnackbarHostState() }
    val dark = when (prefs.themeMode) {
        "light" -> false
        "system" -> isSystemInDarkTheme()
        else -> true
    }
    LaunchedEffect(prefs.highRefreshRate, locked) {
        activity.refreshController.setEnabled(prefs.highRefreshRate && !locked)
    }
    SideEffect {
        if (locked || prefs.privatePreview) activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    LaunchedEffect(Unit) { while (true) { model.refreshDate(); delay(30_000) } }
    LaunchedEffect(model, locked) {
        if (!locked) model.uiEvents.collectLatest { snackbar.showSnackbar(it) }
    }
    NurTheme(prefs) {
        if (locked) {
            LockScreen(lock, authenticate)
        } else {
            CompositionLocalProvider(LocalNurSnackbarHost provides snackbar) {
                val navigate: (String) -> Unit = { route ->
                    if (route != current) {
                        if (route in NurMainTabs.map { it.route }) nav.navigate(route) {
                            popUpTo(nav.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        } else nav.navigate(route) { launchSingleTop = true }
                    }
                }
                val enter = if (prefs.reduceMotion) EnterTransition.None else fadeIn(tween(220))
                val exit = if (prefs.reduceMotion) ExitTransition.None else fadeOut(tween(120))
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = { NurTopBar(current, today, onBack = {
                        if (!nav.popBackStack()) navigate("journey")
                    }, onSettings = { navigate("settings") }) },
                    bottomBar = { if (current in NurMainTabs.map { it.route }) NurBottomBar(current, navigate) },
                    snackbarHost = { SnackbarHost(snackbar) }
                ) { padding ->
                    NavHost(navController = nav, startDestination = "journey", modifier = Modifier.fillMaxSize().padding(padding), enterTransition = { enter }, exitTransition = { exit }, popEnterTransition = { enter }, popExitTransition = { exit }) {
                        composable("journey") { DailyJourneyScreen(entries, completions, today, prefs, model, navigate) }
                        composable("amanah") { EntryScreen("Amanah", "Your daily responsibilities", NurKind.AMANAH, entries, completions, today, model) }
                        composable("muhasaba") { EntryScreen("Muhasaba", "Reflect", NurKind.MUHASABA, entries, completions, today, model) }
                        composable("rhythm") { EntryScreen("Rhythm", "Build consistent habits", NurKind.RHYTHM, entries, completions, today, model) }
                        composable("history") { HistoryScreen(allEntries, completions) }
                        composable("settings") { PowerSettingsScreen(prefs, model, navigate, lock) }
                        composable("appearance") { AppearanceStudio(prefs, model, refreshStatus) }
                        composable("layout") { JourneyStudio(prefs.journey, model) }
                        composable("insights") { InsightsScreen(allEntries, completions, today) }
                        composable("backup") { BackupScreen() }
                        composable("privacy") { PrivacyScreen(lock, prefs.privatePreview) { model.setting("private_preview", it) } }
                        composable("dhikr") { DhikrScreen(model, prefs, today) }
                        composable("reflections?verse={verse}", arguments = listOf(androidx.navigation.navArgument("verse") { type = androidx.navigation.NavType.StringType; defaultValue = "" })) { entry ->
                            ReflectionScreen(entry.arguments?.getString("verse"))
                        }
                    }
                }
            }
        }
    }
}
