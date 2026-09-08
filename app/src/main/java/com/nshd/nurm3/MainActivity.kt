package com.nshd.nurm3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NurApp(model) }
    }
    override fun onResume() {
        super.onResume()
        model.refreshDate()
    }
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
fun NurApp(model: NurViewModel) {
    val prefs by model.preferences.collectAsStateWithLifecycle()
    val entries by model.entries.collectAsStateWithLifecycle()
    val allEntries by model.allEntries.collectAsStateWithLifecycle()
    val completions by model.completions.collectAsStateWithLifecycle()
    val today by model.today.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val current = nav.currentBackStackEntryAsState().value?.destination?.route ?: "journey"
    LaunchedEffect(Unit) { while (true) { model.refreshDate(); delay(30_000) } }
    NurTheme(prefs) {
        val navigate: (String) -> Unit = { route ->
            if (route != current) {
                if (route in tabs.map { it.route }) nav.navigate(route) {
                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                } else nav.navigate(route) { launchSingleTop = true }
            }
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(if (current == "journey") "نُور" else when (current) {
                                "appearance" -> "Appearance Studio"
                                "layout" -> "Customize Journey"
                                "settings" -> "Settings"
                                else -> tabs.firstOrNull { it.route == current }?.label ?: "NUR"
                            }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            if (current == "journey") Text(today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    navigationIcon = { if (current in listOf("settings", "appearance", "layout")) IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                    actions = { if (current !in listOf("settings", "appearance", "layout")) IconButton(onClick = { nav.navigate("settings") }) { Icon(Icons.Default.Settings, contentDescription = "Settings") } }
                )
            },
            bottomBar = {
                if (current in tabs.map { it.route }) NavigationBar {
                    tabs.forEach { tab -> NavigationBarItem(selected = current == tab.route, onClick = { navigate(tab.route) }, icon = { Icon(tab.icon, contentDescription = null) }, label = { Text(tab.label) }, alwaysShowLabel = false) }
                }
            }
        ) { padding ->
            NavHost(navController = nav, startDestination = "journey", modifier = Modifier.fillMaxSize().padding(padding)) {
                composable("journey") { DailyJourneyScreen(entries, completions, today, prefs, model, navigate) }
                composable("amanah") { EntryScreen("Amanah", "Your daily responsibilities", NurKind.AMANAH, entries, completions, today, model) }
                composable("muhasaba") { EntryScreen("Muhasaba", "Reflect on your day", NurKind.MUHASABA, entries, completions, today, model) }
                composable("rhythm") { EntryScreen("Rhythm", "Build consistent habits", NurKind.RHYTHM, entries, completions, today, model) }
                composable("history") { HistoryScreen(allEntries, completions) }
                composable("settings") { SettingsScreen(prefs, model, navigate) }
                composable("appearance") { AppearanceStudio(prefs, model) }
                composable("layout") { JourneyStudio(prefs.journey, model) }
            }
        }
    }
}
