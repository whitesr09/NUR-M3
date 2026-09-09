package com.nshd.nurm3.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.data.NurPreferences
import com.nshd.nurm3.data.NurLock

@Composable
fun PowerSettingsScreen(prefs: NurPreferences, model: NurViewModel, navigate: (String) -> Unit, lock: NurLock) {
    val configured by lock.configuredFlow.collectAsState()
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Make NUR yours", style = MaterialTheme.typography.headlineMedium)
            Text("Your daily experience, your data, your choices.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { PowerLink(Icons.Default.Palette, "Appearance Studio", "Colors, typography, density and motion") { navigate("appearance") } }
        item { PowerLink(Icons.Default.ViewAgenda, "Daily Journey", "Reorder, hide and arrange your dashboard") { navigate("layout") } }
        item { PowerLink(Icons.Default.MenuBook, "Quran Reflections", "Read a curated collection offline, with references") { navigate("reflections") } }
        item { PowerLink(Icons.Default.TouchApp, "Dhikr", "Offline counter, personal goals and saved history") { navigate("dhikr") } }
        item { PowerLink(Icons.Default.Insights, "Insights", "Real completion history and habit streaks") { navigate("insights") } }
        item { PowerLink(Icons.Default.Backup, "Backup & restore", "Export, preview, merge and recover your records") { navigate("backup") } }
        item { PowerLink(Icons.Default.Lock, "Privacy & app lock", if (configured) "App lock enabled" else "PIN, device authentication and private previews") { navigate("privacy") } }
        item {
            Text("Privacy & reliability", style = MaterialTheme.typography.titleLarge)
            Text("Local entries are retained across daily resets. Export a backup before changing signing certificates or reinstalling.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { SettingToggle("Private previews", "Hide personal text in screenshots and recent apps", prefs.privatePreview) { model.setting("private_preview", it) } }
        item { SettingToggle("Reduce motion", "Use simpler transitions and progress changes", prefs.reduceMotion) { model.setting("motion", it) } }
        item { SettingToggle("Backup reminders", "Show reminders before important updates", prefs.backupReminders) { model.setting("backup_reminders", it) } }
        item {
            HorizontalDivider()
            Text("NUR-M3 • Development source", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
            Text("Made by NSHD", style = MaterialTheme.typography.labelSmall)
            Text("Other companion modules and NUR AI will appear here only when their functional implementations are ready.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PowerLink(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}
