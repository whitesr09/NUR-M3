package com.nshd.nurm3.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.data.NurPreferences
import com.nshd.nurm3.data.NurLock

@Composable
fun PowerSettingsScreen(prefs: NurPreferences, model: NurViewModel, navigate: (String) -> Unit, lock: NurLock) {
    val configured by lock.configuredFlow.collectAsState()
    LazyColumn(contentPadding = PaddingValues(NurDesign.pagePadding), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { NurPageHeading("Your preferences", "Make NUR yours", "Your daily experience, your data, your choices.") }
        item { Text("YOUR SPACE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
        item { PowerLink(Icons.Default.Palette, "Appearance Studio", "Colors, typography, density and motion") { navigate("appearance") } }
        item { PowerLink(Icons.Default.ViewAgenda, "Daily Journey", "Reorder, hide and arrange your dashboard") { navigate("layout") } }
        item { Text("COMPANION TOOLS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
        item { PowerLink(Icons.Default.MenuBook, "Quran Reflections", "Offline reading with references") { navigate("reflections") } }
        item { PowerLink(Icons.Default.TouchApp, "Dhikr", "Personal counting and saved history") { navigate("dhikr") } }
        item { PowerLink(Icons.Default.Insights, "Insights", "Genuine activity and habit streaks") { navigate("insights") } }
        item { Text("PRIVACY & RELIABILITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
        item { PowerLink(Icons.Default.Backup, "Backup & restore", "Export, preview, merge and recover your records") { navigate("backup") } }
        item { PowerLink(Icons.Default.Lock, "Privacy & app lock", if (configured) "App lock enabled" else "PIN, device authentication and private previews") { navigate("privacy") } }
        item { SettingToggle("Private previews", "Hide personal text in screenshots and recent apps", prefs.privatePreview) { model.setting("private_preview", it) } }
        item { SettingToggle("Reduce motion", "Use simpler transitions and progress changes", prefs.reduceMotion) { model.setting("motion", it) } }
        item { SettingToggle("Backup reminders", "Show reminders before important updates", prefs.backupReminders) { model.setting("backup_reminders", it) } }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Your data stays yours", style = MaterialTheme.typography.titleSmall)
                }
                Text("Local entries remain saved across daily resets. Export a backup before changing signing certificates or reinstalling. Other companion modules and NUR AI will appear only when their functional implementations are ready.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("نُور", style = MaterialTheme.typography.titleLarge, color = NurDesign.gold)
                Text("NUR-M3 • Development source", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Made by NSHD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PowerLink(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = scheme.let { MaterialTheme.shapes.large }, color = scheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.65f))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = MaterialTheme.shapes.medium, color = scheme.primary.copy(alpha = 0.10f)) {
                Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(21.dp)) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}
