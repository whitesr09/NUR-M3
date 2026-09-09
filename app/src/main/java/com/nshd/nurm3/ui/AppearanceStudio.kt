package com.nshd.nurm3.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.data.*
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceStudio(prefs: NurPreferences, model: NurViewModel) {
    var confirmReset by remember { mutableStateOf(false) }
    var previewDone by rememberSaveable { mutableStateOf(false) }
    val preview = remember { Entry("appearance-preview", NurKind.AMANAH, "Read 10 pages", 0, 0L, startDate = LocalDate.now().toString()) }
    LazyColumn(contentPadding = PaddingValues(NurDesign.pagePadding), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { NurPageHeading("Personalize", "Appearance Studio", "Make NUR feel like yours. Every change is saved automatically.") }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Text("LIVE PREVIEW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text("نُور", style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Serif), color = NurDesign.gold, fontWeight = FontWeight.Bold)
                Text("A calmer way to care for your day.", style = MaterialTheme.typography.bodyMedium)
                NurLinearProgress(0.65f, LocalDate.MIN, "Preview progress")
                ChecklistRow(preview, previewDone, { previewDone = it }, date = LocalDate.MIN)
                Text("The sample checklist is interactive and does not change your saved data.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Text("Appearance mode", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { option ->
                    NurChoicePill(option.second, prefs.themeMode == option.first, { model.choice("mode", option.first) })
                }
            }
        }
        item {
            Text("Color palette", style = MaterialTheme.typography.titleMedium)
            Text("Choose a signature accent or use your wallpaper's colors.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NurPaletteOption("Signature gold", Color(0xFFD9B96F), prefs.palette == "gold" && !prefs.dynamicColor, { model.choice("palette", "gold"); model.setting("dynamic", false) }, Modifier.weight(1f))
                    NurPaletteOption("Ocean blue", Color(0xFF638FA9), prefs.palette == "ocean" && !prefs.dynamicColor, { model.choice("palette", "ocean"); model.setting("dynamic", false) }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NurPaletteOption("Sage green", Color(0xFF71977D), prefs.palette == "sage" && !prefs.dynamicColor, { model.choice("palette", "sage"); model.setting("dynamic", false) }, Modifier.weight(1f))
                    NurPaletteOption("Soft rose", Color(0xFFB98091), prefs.palette == "rose" && !prefs.dynamicColor, { model.choice("palette", "rose"); model.setting("dynamic", false) }, Modifier.weight(1f))
                }
            }
        }
        item { SettingToggle("Dynamic color", "Use wallpaper colors on Android 12 and newer", prefs.dynamicColor, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { model.setting("dynamic", it) } }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Typography size", style = MaterialTheme.typography.titleSmall)
                    Text("${(prefs.typeScale * 100).toInt()}%", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }
                Slider(value = prefs.typeScale, onValueChange = { model.typeScale(it) }, valueRange = 0.85f..1.2f, steps = 6)
                Text("Adjust text size without changing your saved content.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { SettingToggle("Compact cards", "Fit more information on smaller screens", prefs.compactCards) { model.setting("compact", it) } }
        item { SettingToggle("Soft shapes", "Switch between rounded and precise corners", prefs.softShapes) { model.setting("shapes", it) } }
        item { SettingToggle("Reduce motion", "Minimize decorative animations and transitions", prefs.reduceMotion) { model.setting("motion", it) } }
        item { SettingToggle("Arabic calligraphy", "Show Arabic text in Daily Light", prefs.showArabic) { model.setting("arabic", it) } }
        item { OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = MaterialTheme.shapes.medium) { Icon(Icons.Default.RestartAlt, null); Spacer(Modifier.width(8.dp)); Text("Reset appearance") } }
    }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false }, title = { Text("Reset appearance?") }, text = { Text("Restore the default theme and Journey layout. Your entries, Dhikr counts and history will not be changed.") }, confirmButton = { TextButton(onClick = { model.resetAppearance(); confirmReset = false }) { Text("Reset") } }, dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } })
}

@Composable
private fun NurPaletteOption(title: String, swatch: Color, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.medium
    Surface(onClick = onClick, modifier = modifier.heightIn(min = 70.dp), shape = shape, color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.size(24.dp).background(swatch, CircleShape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            if (selected) Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun SettingToggle(title: String, description: String, value: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    NurSettingRow(title, description, value, enabled, onChange)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JourneyStudio(layout: JourneyLayout, model: NurViewModel) {
    LazyColumn(contentPadding = PaddingValues(NurDesign.pagePadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { NurPageHeading("Your dashboard", "Build your daily flow", "Reorder or hide cards without deleting any saved entry.") }
        item {
            Text("Presets", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Balanced" to JourneyLayout.DEFAULT, "Prayer first" to JourneyLayout.PRAYER_FIRST, "Focus" to JourneyLayout.FOCUS).forEach { option ->
                    NurChoicePill(option.first, layout == option.second, { model.journey(option.second) })
                }
            }
        }
        items(layout.order, key = { it }) { id ->
            val index = layout.order.indexOf(id)
            val visible = id !in layout.hidden
            NurPanel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(JourneyCard.titles[id] ?: id, style = MaterialTheme.typography.titleSmall)
                        Text(if (visible) "Visible" else "Hidden", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = visible, onCheckedChange = { model.journey(layout.show(id, it)) }, enabled = !visible || layout.visible().size > 1)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = { model.journey(layout.move(id, -1)) }, enabled = index > 0, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move ${JourneyCard.titles[id]} up") }
                    IconButton(onClick = { model.journey(layout.move(id, 1)) }, enabled = index < layout.order.lastIndex, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move ${JourneyCard.titles[id]} down") }
                }
            }
        }
        item { Text("At least one card remains visible. Hiding or moving a card never changes your completion records.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
