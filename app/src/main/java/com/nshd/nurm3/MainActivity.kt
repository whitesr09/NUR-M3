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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nshd.nurm3.data.*
import com.nshd.nurm3.ui.NurTheme
import java.time.LocalDate
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

private enum class Destination(val label: String) { HOME("Home"), AMANAH("Amanah"), MUHASABA("Muhasaba"), HISTORY("History"), SETTINGS("Settings") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NurApp(model: NurViewModel) {
    val prefs by model.preferences.collectAsStateWithLifecycle()
    val entries by model.entries.collectAsStateWithLifecycle()
    val completions by model.completions.collectAsStateWithLifecycle()
    val today by model.today.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
    LaunchedEffect(Unit) {
        while (true) {
            model.refreshDate()
            delay(30_000)
        }
    }
    NurTheme(prefs) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Column {
                    Text("نُور", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), style = MaterialTheme.typography.labelSmall)
                } }, actions = {
                    IconButton(onClick = { destination = Destination.SETTINGS }) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                })
            },
            bottomBar = {
                NavigationBar {
                    listOf(Destination.HOME, Destination.AMANAH, Destination.MUHASABA, Destination.HISTORY).forEach { item ->
                        val icon = when (item) {
                            Destination.HOME -> Icons.Default.Home
                            Destination.AMANAH -> Icons.Default.Checklist
                            Destination.MUHASABA -> Icons.Default.EditNote
                            Destination.HISTORY -> Icons.Default.History
                            Destination.SETTINGS -> Icons.Default.Settings
                        }
                        NavigationBarItem(selected = destination == item, onClick = { destination = item }, icon = { Icon(icon, contentDescription = null) }, label = { Text(item.label) })
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                when (destination) {
                    Destination.HOME -> HomeScreen(entries, completions, today, prefs, model)
                    Destination.AMANAH -> EntryScreen("Amanah", "Your daily responsibilities", NurKind.AMANAH, entries, completions, today, model)
                    Destination.MUHASABA -> EntryScreen("Muhasaba", "Reflect on your day", NurKind.MUHASABA, entries, completions, today, model)
                    Destination.HISTORY -> HistoryScreen(entries, completions, today)
                    Destination.SETTINGS -> SettingsScreen(prefs, model)
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(entries: List<Entry>, completions: List<Completion>, today: LocalDate, prefs: NurPreferences, model: NurViewModel) {
    val prayers = entries.filter { it.kind == NurKind.PRAYER }
    val tasks = entries.filter { it.kind != NurKind.PRAYER }
    val done = completions.filter { it.localDate == today.toString() }.map { it.entryId }.toSet()
    val total = prayers.size + tasks.size
    val completed = entries.count { it.id in done }
    val progress = if (total == 0) 0f else completed.toFloat() / total
    androidx.compose.foundation.lazy.LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (prefs.showArabic) Text("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Daily Light", style = MaterialTheme.typography.titleLarge)
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(148.dp), strokeWidth = 9.dp)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الله", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Text("$completed of $total completed", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item { Text("Your prayers", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        items(prayers.size) { index ->
            val entry = prayers[index]
            EntryRow(entry, entry.id in done, { model.complete(entry.id, today, it) })
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Amanah & Muhasaba", style = MaterialTheme.typography.titleMedium)
                    Text("${tasks.count { it.id in done }} of ${tasks.size} completed today")
                    LinearProgressIndicator(progress = { if (tasks.isEmpty()) 0f else tasks.count { it.id in done }.toFloat() / tasks.size }, modifier = Modifier.fillMaxWidth())
                    Text("Your entries stay saved. Only daily checkmarks start fresh on the next day.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: Entry, checked: Boolean, onChecked: (Boolean) -> Unit, onDelete: (() -> Unit)? = null) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = onChecked)
            Text(entry.title, Modifier.weight(1f).padding(8.dp), style = MaterialTheme.typography.bodyLarge)
            if (onDelete != null) IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${entry.title}") }
        }
    }
}

@Composable
private fun EntryScreen(title: String, subtitle: String, kind: String, entries: List<Entry>, completions: List<Completion>, today: LocalDate, model: NurViewModel) {
    var draft by rememberSaveable(kind) { mutableStateOf("") }
    var deleting by remember { mutableStateOf<Entry?>(null) }
    val items = entries.filter { it.kind == kind }
    val done = completions.filter { it.localDate == today.toString() }.map { it.entryId }.toSet()
    androidx.compose.foundation.lazy.LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = draft, onValueChange = { draft = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Add an entry") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { model.add(kind, draft); draft = "" }, enabled = draft.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Add") }
        }
        item {
            Text("${items.count { it.id in done }} / ${items.size} completed today", style = MaterialTheme.typography.labelLarge)
            LinearProgressIndicator(progress = { if (items.isEmpty()) 0f else items.count { it.id in done }.toFloat() / items.size }, modifier = Modifier.fillMaxWidth())
        }
        if (items.isEmpty()) item { Text("No entries yet. Add one above to begin.", style = MaterialTheme.typography.bodyMedium) }
        items(items.size) { index ->
            val entry = items[index]
            EntryRow(entry, entry.id in done, { model.complete(entry.id, today, it) }, { deleting = entry })
        }
    }
    deleting?.let { entry ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete entry?") }, text = { Text("${entry.title} will be removed from your active list and its completion records deleted.") }, confirmButton = { TextButton(onClick = { model.delete(entry.id); deleting = null }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } })
    }
}

@Composable
private fun HistoryScreen(entries: List<Entry>, completions: List<Completion>, today: LocalDate) {
    val dates = completions.map { it.localDate }.distinct().sortedDescending()
    androidx.compose.foundation.lazy.LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("History", style = MaterialTheme.typography.headlineMedium); Text("Your recorded activity, never invented or backfilled.") }
        if (dates.isEmpty()) item { Text("No completed days recorded yet.") }
        items(dates.size) { index ->
            val date = dates[index]
            val records = completions.filter { it.localDate == date }
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(date, style = MaterialTheme.typography.titleMedium)
                    Text("${records.size} recorded completions", color = MaterialTheme.colorScheme.primary)
                    records.forEach { record ->
                        val name = entries.firstOrNull { it.id == record.entryId }?.title ?: "Archived entry"
                        Text("• $name", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(prefs: NurPreferences, model: NurViewModel) {
    androidx.compose.foundation.lazy.LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Appearance", style = MaterialTheme.typography.headlineMedium); Text("Make NUR feel like your own.") }
        item { SettingToggle("Dark mode", "Use the deep black-blue palette", prefs.darkMode) { model.setting("dark", it) } }
        item { SettingToggle("Dynamic color", "Use your Android wallpaper colors on Android 12+", prefs.dynamicColor) { model.setting("dynamic", it) } }
        item { SettingToggle("Gold accent", "Use NUR's signature golden color", prefs.goldAccent) { model.setting("gold", it) } }
        item { SettingToggle("Reduce motion", "Preference for minimal animation", prefs.reduceMotion) { model.setting("motion", it) } }
        item { SettingToggle("Arabic header", "Show Bismillah in Daily Light", prefs.showArabic) { model.setting("arabic", it) } }
        item {
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text("NUR AI", style = MaterialTheme.typography.titleLarge)
            Text("Optional Gemini integration is planned for a later milestone. The offline app does not require an API key.")
            Spacer(Modifier.height(8.dp))
            Text("NUR Material 3 • 0.1.0", style = MaterialTheme.typography.labelMedium)
            Text("Made by NSHD", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SettingToggle(title: String, description: String, value: Boolean, onChange: (Boolean) -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = value, onCheckedChange = onChange)
        }
    }
}
