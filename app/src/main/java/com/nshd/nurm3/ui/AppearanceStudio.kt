package com.nshd.nurm3.ui

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.data.NurPreferences

@Composable
fun SettingsScreen(prefs: NurPreferences, model: NurViewModel, navigate: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Make NUR yours", style = MaterialTheme.typography.headlineMedium)
            Text("Personalize your daily experience while keeping your data safe.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { StudioLink(Icons.Default.Palette, "Appearance Studio", "Themes, colors, typography and motion") { navigate("appearance") } }
        item { StudioLink(Icons.Default.ViewAgenda, "Daily Journey", "Choose, arrange and hide dashboard cards") { navigate("layout") } }
        item {
            Text("Privacy & reliability", style = MaterialTheme.typography.titleLarge)
            Text("These controls prepare the stable version for lock, backup, widgets and companion modules.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { SettingToggle("Private preview", "Hide sensitive task/reflection previews outside the app", prefs.privatePreview) { model.setting("private_preview", it) } }
        item { SettingToggle("App lock ready", "Reserve the privacy flow for PIN/biometric lock", prefs.appLock) { model.setting("app_lock", it) } }
        item { SettingToggle("Backup reminders", "Remind users to export data before major updates", prefs.backupReminders) { model.setting("backup_reminders", it) } }
        item { SettingToggle("Widgets", "Enable home-screen widget support", prefs.widgetsEnabled) { model.setting("widgets", it) } }
        item { SettingToggle("Companion modules", "Show optional Islamic companion tools in future releases", prefs.companionModules) { model.setting("companion_modules", it) } }
        item { SettingToggle("NUR AI", "Enable the future Gemini BYOK assistant module", prefs.nurAiEnabled) { model.setting("nur_ai", it) } }
        item {
            HorizontalDivider()
            Text("NUR AI", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
            Text("Optional Gemini integration will use a Bring Your Own Key setup. No shared API key is shipped in the APK.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Text("NUR Material 3 • 0.3.0", style = MaterialTheme.typography.labelMedium)
            Text("Made by NSHD", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun StudioLink(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
fun AppearanceStudio(prefs: NurPreferences, model: NurViewModel) {
    var confirmReset by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Text("Your personal NUR", style = MaterialTheme.typography.headlineMedium)
            Text("Every change is saved automatically.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Live preview", style = MaterialTheme.typography.titleMedium)
                    Text("نُور", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                    Text("A calmer way to care for your day.", style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(progress = { 0.65f }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small) { Text("Primary", Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onPrimary) }
                        Surface(border = ButtonDefaults.outlinedButtonBorder, shape = MaterialTheme.shapes.small) { Text("Outlined", Modifier.padding(12.dp)) }
                    }
                }
            }
        }
        item {
            Text("Appearance mode", style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEachIndexed { index, option ->
                    SegmentedButton(shape = SegmentedButtonDefaults.itemShape(index, 3), onClick = { model.choice("mode", option.first) }, selected = prefs.themeMode == option.first) { Text(option.second) }
                }
            }
        }
        item {
            Text("Color palette", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("gold" to "Signature gold", "ocean" to "Ocean blue", "sage" to "Sage green", "rose" to "Soft rose").forEach { option ->
                    FilterChip(selected = prefs.palette == option.first && !prefs.dynamicColor, onClick = { model.choice("palette", option.first); model.setting("dynamic", false) }, label = { Text(option.second) }, leadingIcon = if (prefs.palette == option.first && !prefs.dynamicColor) ({ Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }) else null)
                }
            }
        }
        item { SettingToggle("Dynamic color", "Use wallpaper colors on Android 12 and newer", prefs.dynamicColor, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { model.setting("dynamic", it) } }
        item {
            Text("Typography size", style = MaterialTheme.typography.titleMedium)
            Text("${(prefs.typeScale * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Slider(value = prefs.typeScale, onValueChange = { model.typeScale(it) }, valueRange = 0.85f..1.2f, steps = 6)
        }
        item { SettingToggle("Compact cards", "More information, less vertical space", prefs.compactCards) { model.setting("compact", it) } }
        item { SettingToggle("Soft shapes", "Use rounded, expressive Material 3 corners", prefs.softShapes) { model.setting("shapes", it) } }
        item { SettingToggle("Reduce motion", "Minimize animated transitions and progress", prefs.reduceMotion) { model.setting("motion", it) } }
        item { SettingToggle("Arabic calligraphy", "Show Arabic text in Daily Light", prefs.showArabic) { model.setting("arabic", it) } }
        item { OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) { Text("Reset appearance") } }
    }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false }, title = { Text("Reset appearance?") }, text = { Text("Restore the default theme and Journey layout. Your entries and history will not be changed.") }, confirmButton = { TextButton(onClick = { model.resetAppearance(); confirmReset = false }) { Text("Reset") } }, dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } })
}

@Composable
fun SettingToggle(title: String, description: String, value: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = value, onCheckedChange = onChange, enabled = enabled)
        }
    }
}

@Composable
fun JourneyStudio(layout: JourneyLayout, model: NurViewModel) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Build your daily flow", style = MaterialTheme.typography.headlineMedium)
            Text("Reorder cards with the arrows, or choose what appears on your home screen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Text("Presets", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { model.journey(JourneyLayout.DEFAULT) }, label = { Text("Balanced") })
                AssistChip(onClick = { model.journey(JourneyLayout.PRAYER_FIRST) }, label = { Text("Prayer first") })
            }
            AssistChip(onClick = { model.journey(JourneyLayout.FOCUS) }, label = { Text("Focus") })
        }
        items(layout.order, key = { it }) { id ->
            val index = layout.order.indexOf(id)
            val visible = id !in layout.hidden
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = visible, onCheckedChange = { model.journey(layout.show(id, it)) }, enabled = !visible || layout.visible().size > 1)
                    Spacer(Modifier.width(8.dp))
                    Text(JourneyCard.titles[id] ?: id, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    IconButton(onClick = { model.journey(layout.move(id, -1)) }, enabled = index > 0) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move ${JourneyCard.titles[id]} up") }
                    IconButton(onClick = { model.journey(layout.move(id, 1)) }, enabled = index < layout.order.lastIndex) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move ${JourneyCard.titles[id]} down") }
                }
            }
        }
        item { Text("Hiding a card never deletes its entries or completion records.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
