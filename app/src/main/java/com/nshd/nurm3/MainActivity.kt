package com.nshd.nurm3

import android.app.BiometricPrompt
import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.nshd.nurm3.data.*
import com.nshd.nurm3.ui.*
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val model: NurViewModel by viewModels()
    private val privateStore by lazy { NurPrivateStore(this) }
    private val lock by lazy { NurLock(privateStore) }
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
                    .setAllowedAuthenticators(BiometricPrompt.Authenticators.BIOMETRIC_STRONG or BiometricPrompt.Authenticators.DEVICE_CREDENTIAL)
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
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent { NurApp(model, lock, ::authenticateDevice) }
    }
    override fun onResume() { super.onResume(); model.refreshDate() }
    override fun onStop() { lock.lock(); super.onStop() }
    override fun onDestroy() { authResult = null; super.onDestroy() }
}

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
private val tabs = listOf(
    Tab("journey", "Journey", Icons.Default.Home),
    Tab("amanah", "Amanah", Icons.Default.Checklist),
    Tab("muhasaba", "Reflect", Icons.Default.EditNote),
    Tab("rhythm", "Rhythm", Icons.Default.Repeat),
    Tab("history", "History", Icons.Default.History)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NurApp(model: NurViewModel, lock: NurLock, authenticate: ((Boolean, String) -> Unit) -> Unit) {
    val prefs by model.preferences.collectAsStateWithLifecycle()
    val entries by model.entries.collectAsStateWithLifecycle()
    val allEntries by model.allEntries.collectAsStateWithLifecycle()
    val completions by model.completions.collectAsStateWithLifecycle()
    val today by model.today.collectAsStateWithLifecycle()
    val locked by lock.locked.collectAsStateWithLifecycle()
    val activity = androidx.compose.ui.platform.LocalContext.current as MainActivity
    val nav = rememberNavController()
    val current = nav.currentBackStackEntryAsState().value?.destination?.route ?: "journey"
    val auxiliary = setOf("settings", "appearance", "layout", "insights", "backup", "privacy")
    SideEffect {
        if (locked || prefs.privatePreview) activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
    LaunchedEffect(Unit) { while (true) { model.refreshDate(); delay(30_000) } }
    NurTheme(prefs) {
        if (locked) {
            LockScreen(lock, authenticate)
        } else {
            val navigate: (String) -> Unit = { route ->
                if (route != current) {
                    if (route in tabs.map { it.route }) nav.navigate(route) {
                        popUpTo(nav.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    } else nav.navigate(route) { launchSingleTop = true }
                }
            }
            val enter = if (prefs.reduceMotion) EnterTransition.None else fadeIn(tween(220))
            val exit = if (prefs.reduceMotion) ExitTransition.None else fadeOut(tween(120))
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(if (current == "journey") "نُور" else when (current) {
                                    "appearance" -> "Appearance Studio"
                                    "layout" -> "Customize Journey"
                                    "settings" -> "Settings"
                                    "insights" -> "Insights"
                                    "backup" -> "Backup & restore"
                                    "privacy" -> "Privacy"
                                    else -> tabs.firstOrNull { it.route == current }?.label ?: "NUR"
                                }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                if (current == "journey") Text(today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        navigationIcon = { if (current in auxiliary) IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                        actions = { if (current !in auxiliary) IconButton(onClick = { navigate("settings") }) { Icon(Icons.Default.Settings, contentDescription = "Settings") } }
                    )
                },
                bottomBar = {
                    if (current in tabs.map { it.route }) NavigationBar {
                        tabs.forEach { tab -> NavigationBarItem(selected = current == tab.route, onClick = { navigate(tab.route) }, icon = { Icon(tab.icon, contentDescription = null) }, label = { Text(tab.label) }, alwaysShowLabel = false) }
                    }
                }
            ) { padding ->
                NavHost(navController = nav, startDestination = "journey", modifier = Modifier.fillMaxSize().padding(padding), enterTransition = { enter }, exitTransition = { exit }, popEnterTransition = { enter }, popExitTransition = { exit }) {
                    composable("journey") { DailyJourneyScreen(entries, completions, today, prefs, model, navigate) }
                    composable("amanah") { EntryScreen("Amanah", "Your daily responsibilities", NurKind.AMANAH, entries, completions, today, model) }
                    composable("muhasaba") { EntryScreen("Muhasaba", "Reflect on your day", NurKind.MUHASABA, entries, completions, today, model) }
                    composable("rhythm") { EntryScreen("Rhythm", "Build consistent habits", NurKind.RHYTHM, entries, completions, today, model) }
                    composable("history") { HistoryScreen(allEntries, completions) }
                    composable("settings") { PowerSettingsScreen(prefs, model, navigate, lock) }
                    composable("appearance") { AppearanceStudio(prefs, model) }
                    composable("layout") { JourneyStudio(prefs.journey, model) }
                    composable("insights") { InsightsScreen(allEntries, completions, today) }
                    composable("backup") { BackupScreen() }
                    composable("privacy") { PrivacyScreen(lock, prefs.privatePreview) { model.setting("private_preview", it) } }
                }
            }
        }
    }
}
