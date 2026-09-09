package com.nshd.nurm3.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.data.*
import java.time.LocalDate

private data class VisualChoice(val id: String, val label: String, val icon: ImageVector)
private val modes = listOf(
    VisualChoice("system", "Follow system", Icons.Default.SettingsBrightness),
    VisualChoice("light", "Light", Icons.Default.LightMode),
    VisualChoice("dark", "Dark", Icons.Default.DarkMode),
    VisualChoice("amoled", "AMOLED", Icons.Default.Contrast)
)
private val progressChoices = listOf("slim" to "Slim", "thick" to "Thick", "wavy" to "Wavy", "squiggly" to "Squiggly")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceStudio(prefs: NurPreferences, model: NurViewModel, refreshStatus: String = "System controlled") {
    var confirmReset by remember { mutableStateOf(false) }
    var picker by rememberSaveable { mutableStateOf("") }
    var previewDone by rememberSaveable { mutableStateOf(false) }
    val preview = remember { Entry("appearance-preview", NurKind.AMANAH, "Read 10 pages", 0, 0L, startDate = LocalDate.now().toString()) }
    LazyColumn(contentPadding = PaddingValues(NurDesign.pagePadding), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { NurPageHeading("Personalize", "Appearance Studio", "Choose a visual style that feels comfortable, clear and uniquely yours.") }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Text("LIVE PREVIEW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text("نُور", style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Serif), color = NurDesign.gold, fontWeight = FontWeight.Bold)
                Text("A calmer way to care for your day.", style = MaterialTheme.typography.bodyMedium)
                NurLinearProgress(0.65f, LocalDate.MIN, "Preview progress")
                ChecklistRow(preview, previewDone, { previewDone = it }, date = LocalDate.MIN)
                Text("Try the sample checkbox. Preview interactions do not change your records.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            AppearanceSection("Theme & colors") {
                NurAppearanceLink(Icons.Default.Palette, "Theme mode", modes.firstOrNull { it.id == prefs.themeMode }?.label ?: "Dark") { picker = "theme" }
                NurAppearanceLink(Icons.Default.ColorLens, "Color palette", if (prefs.dynamicColor) "Dynamic wallpaper colors" else prefs.palette.replaceFirstChar { it.uppercase() }) { picker = "palette" }
                NurSettingRow("Dynamic colors", "Use wallpaper colors on Android 12 or newer", prefs.dynamicColor, android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) { model.setting("dynamic", it) }
            }
        }
        item {
            AppearanceSection("Progress & motion") {
                NurAppearanceLink(Icons.Default.Timeline, "Progression style", progressChoices.first { it.first == prefs.progressStyle }.second) { picker = "progress" }
                NurSettingRow("Reduce motion", "Minimize decorative transitions and animations", prefs.reduceMotion) { model.setting("motion", it) }
            }
        }
        item {
            AppearanceSection("Display") {
                NurSettingRow("Maximize refresh rate", "Request the highest compatible display mode up to 120 Hz", prefs.highRefreshRate) { model.setting("high_refresh_rate", it) }
                Text(refreshStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Android may choose a lower rate for battery, heat or system policy. This setting only affects NUR while it is open.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                NurSettingRow("Compact cards", "Fit more information on smaller screens", prefs.compactCards) { model.setting("compact", it) }
                NurSettingRow("Soft shapes", "Switch between rounded and precise corners", prefs.softShapes) { model.setting("shapes", it) }
            }
        }
        item {
            AppearanceSection("Typography & details") {
                NurPanel(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Typography size", style = MaterialTheme.typography.titleSmall)
                        Text("${(prefs.typeScale * 100).toInt()}%", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    }
                    Slider(value = prefs.typeScale, onValueChange = { model.typeScale(it) }, valueRange = 0.85f..1.2f, steps = 6)
                    Text("Adjust text size without changing your content.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                NurSettingRow("Arabic calligraphy", "Show Arabic text in Daily Light", prefs.showArabic) { model.setting("arabic", it) }
            }
        }
        item { OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = MaterialTheme.shapes.medium) { Icon(Icons.Default.RestartAlt, null); Spacer(Modifier.width(8.dp)); Text("Reset appearance") } }
    }
    when (picker) {
        "theme" -> AppearancePicker("Theme mode", onDismiss = { picker = "" }) {
            Text("Choose how NUR follows your display. AMOLED uses true black backgrounds and surfaces.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    modes.take(2).forEach { option -> ThemeTile(option, prefs.themeMode == option.id, { model.choice("mode", option.id); picker = "" }, Modifier.weight(1f)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    modes.drop(2).forEach { option -> ThemeTile(option, prefs.themeMode == option.id, { model.choice("mode", option.id); picker = "" }, Modifier.weight(1f)) }
                }
            }
        }
        "progress" -> AppearancePicker("Progression style", onDismiss = { picker = "" }) {
            Text("A visual choice only. All styles use the same saved progress values.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                progressChoices.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { (id, label) ->
                            Surface(onClick = { model.choice("progress_style", id); picker = "" }, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.large, color = if (prefs.progressStyle == id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, if (prefs.progressStyle == id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                                Column(Modifier.padding(12.dp).heightIn(min = 126.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
                                    NurLinearProgress(0.68f, LocalDate.MIN, "${label} preview", style = id)
                                    Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    if (prefs.progressStyle == id) Icon(Icons.Default.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
        "palette" -> AppearancePicker("Color palette", onDismiss = { picker = "" }) {
            Text("Choose a signature accent, or enable Dynamic colors to use your wallpaper.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(Triple("gold", "Signature gold", Color(0xFFD9B96F)), Triple("ocean", "Ocean blue", Color(0xFF638FA9)), Triple("sage", "Sage green", Color(0xFF71977D)), Triple("rose", "Soft rose", Color(0xFFB98091))).forEach { (id, title, swatch) ->
                NurPaletteOption(title, swatch, prefs.palette == id && !prefs.dynamicColor, { model.choice("palette", id); model.setting("dynamic", false); picker = "" }, Modifier.fillMaxWidth())
            }
        }
    }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false }, title = { Text("Reset appearance?") }, text = { Text("Restore default theme, progression and Journey layout. Your entries, Dhikr counts and history will not change.") }, confirmButton = { TextButton(onClick = { model.resetAppearance(); confirmReset = false }) { Text("Reset") } }, dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } })
}

@Composable
private fun AppearanceSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun NurAppearanceLink(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = scheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.65f))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = MaterialTheme.shapes.medium, color = scheme.primary.copy(alpha = 0.10f)) { Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) { Icon(icon, null, tint = scheme.primary) } }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = scheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearancePicker(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            content()
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Cancel") }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ThemeTile(option: VisualChoice, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(onClick = onClick, modifier = modifier.heightIn(min = 132.dp), shape = MaterialTheme.shapes.large, color = if (selected) scheme.primaryContainer else scheme.surface, border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(option.icon, null, modifier = Modifier.size(28.dp), tint = if (selected) scheme.primary else scheme.onSurfaceVariant)
            Text(option.label, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            if (selected) Icon(Icons.Default.CheckCircle, "Selected", tint = scheme.primary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun NurPaletteOption(title: String, swatch: Color, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(onClick = onClick, modifier = modifier.heightIn(min = 64.dp), shape = MaterialTheme.shapes.medium, color = if (selected) scheme.primaryContainer else scheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) scheme.primary else scheme.outlineVariant)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = swatch, shape = MaterialTheme.shapes.small, modifier = Modifier.size(30.dp)) { }
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            if (selected) Icon(Icons.Default.CheckCircle, "Selected", tint = scheme.primary)
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
