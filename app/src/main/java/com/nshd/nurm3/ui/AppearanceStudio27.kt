package com.nshd.nurm3.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.nshd.nurm3.data.NurPreferences
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Appearance Studio 0.27. Font and glass settings live in their semantic sections instead of
 * being pinned above the scrolling studio.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppearanceStudio27(
    prefs: NurPreferences,
    model: NurViewModel,
    refreshStatus: String,
    navigate: (String) -> Unit
) {
    var sheet by rememberSaveable { mutableStateOf("") }
    var previewDone by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(NurDesign.pagePadding),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            NurPageHeading(
                "Personalize",
                "Appearance Studio",
                "Tune color, typography, glass depth, navigation and motion without changing your data."
            )
        }

        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Text("LIVE PREVIEW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text("نُور", style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Serif), color = NurDesign.gold, fontWeight = FontWeight.Bold)
                Text("A little light, every day.", style = MaterialTheme.typography.bodyMedium)
                NurLinearProgress(0.68f, LocalDate.MIN, "Appearance preview", style = prefs.progressStyle)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Checkbox(checked = previewDone, onCheckedChange = { previewDone = it })
                    Text(if (previewDone) "Preview complete" else "Tap to preview interaction")
                }
            }
        }

        item {
            StudioSection27("Theme & colors") {
                StudioLink27(Icons.Default.Palette, "Theme mode", prefs.themeMode.replaceFirstChar { it.uppercase() }) { sheet = "theme" }
                StudioLink27(
                    Icons.Default.ColorLens,
                    "Color palette",
                    if (prefs.dynamicColor) "Dynamic wallpaper colors" else prefs.palette.replaceFirstChar { it.uppercase() }
                ) { sheet = "palette" }
                NurSettingRow("Dynamic colors", "Use wallpaper colors on Android 12 or newer", prefs.dynamicColor, android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    model.setting("dynamic", it)
                }
            }
        }

        item {
            StudioSection27("Text & typography") {
                NurFontSettingsLink(prefs) { navigate("fonts") }
                NurPanel(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Typography size", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("${(prefs.typeScale * 100).roundToInt()}%", color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(value = prefs.typeScale, onValueChange = model::typeScale, valueRange = 0.85f..1.2f, steps = 6)
                    Text("Scales NUR text while keeping the interface responsive.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                NurSettingRow("Arabic calligraphy", "Show Arabic text in Daily Light and spiritual details", prefs.showArabic) {
                    model.setting("arabic", it)
                }
            }
        }

        item {
            StudioSection27("Progress & motion") {
                StudioLink27(Icons.Default.Timeline, "Progression style", prefs.progressStyle.replaceFirstChar { it.uppercase() }) { sheet = "progress" }
                NurSettingRow("Reduce motion", "Minimize decorative transitions, sheen and animations", prefs.reduceMotion) {
                    model.setting("motion", it)
                }
            }
        }

        item {
            StudioSection27("Display") {
                StudioLink27(
                    Icons.Default.BlurOn,
                    "Glass & depth",
                    when (prefs.glassMode) {
                        "liquid" -> "Liquid glass · ${prefs.glassBlurRadius}px depth"
                        "frosted" -> "Frosted glass · ${prefs.glassBlurRadius}px depth"
                        "subtle" -> "Subtle translucent surfaces"
                        else -> "Solid Material surfaces"
                    }
                ) { sheet = "glass" }
                StudioLink27(
                    Icons.Default.ViewWeek,
                    "Bottom navigation",
                    resolveNurTabs(prefs.bottomNavigation).joinToString(" · ") { it.label }
                ) { navigate("navigation") }
                NurSettingRow("Maximize refresh rate", "Request the highest compatible display mode up to 120 Hz", prefs.highRefreshRate) {
                    model.setting("high_refresh_rate", it)
                }
                Text(refreshStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                NurSettingRow("Compact cards", "Fit more information on smaller screens", prefs.compactCards) { model.setting("compact", it) }
                NurSettingRow("Soft shapes", "Use rounded Material shapes throughout NUR", prefs.softShapes) { model.setting("shapes", it) }
            }
        }

        item {
            OutlinedButton(
                onClick = model::resetAppearance,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                Icon(Icons.Default.RestartAlt, null)
                Spacer(Modifier.width(8.dp))
                Text("Reset appearance")
            }
        }
        item { Spacer(Modifier.height(10.dp)) }
    }

    if (sheet.isNotBlank()) {
        ModalBottomSheet(onDismissRequest = { sheet = "" }) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (sheet) {
                    "theme" -> {
                        Text("Theme mode", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        Text("Choose your base. AMOLED uses true black surfaces.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        listOf("system" to "Follow system", "light" to "Light", "dark" to "Dark", "amoled" to "AMOLED black").forEach { (id, label) ->
                            ChoiceRow27(label, prefs.themeMode == id) { model.choice("mode", id); sheet = "" }
                        }
                    }
                    "palette" -> {
                        Text("Color palette", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        listOf("gold" to "Signature gold", "ocean" to "Ocean blue", "sage" to "Sage green", "rose" to "Soft rose").forEach { (id, label) ->
                            ChoiceRow27(label, prefs.palette == id && !prefs.dynamicColor) {
                                model.choice("palette", id); model.setting("dynamic", false); sheet = ""
                            }
                        }
                    }
                    "progress" -> {
                        Text("Progression style", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        listOf("slim", "thick", "wavy", "squiggly").forEach { id ->
                            Surface(
                                onClick = { model.choice("progress_style", id); sheet = "" },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                color = if (prefs.progressStyle == id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    NurLinearProgress(0.68f, LocalDate.MIN, "$id preview", style = id)
                                    Text(id.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    "glass" -> {
                        Text("Glass & depth", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Fixed chrome, navigation and the NUR AI button use stronger translucent glass. Scrolling content can pass underneath the fixed surfaces.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("off" to "Off", "subtle" to "Subtle", "frosted" to "Frosted", "liquid" to "Liquid").forEach { (id, label) ->
                                FilterChip(selected = prefs.glassMode == id, onClick = { model.choice("glass_mode", id) }, label = { Text(label) })
                            }
                        }
                        NurPanel(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Glass intensity", style = MaterialTheme.typography.titleSmall)
                                Text("${(prefs.glassIntensity * 100).roundToInt()}%", color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(value = prefs.glassIntensity, onValueChange = model::glassIntensity, valueRange = 0.25f..1f, enabled = prefs.glassMode != "off")
                        }
                        NurPanel(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Blur depth", style = MaterialTheme.typography.titleSmall)
                                Text("${prefs.glassBlurRadius}px", color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = prefs.glassBlurRadius.toFloat(),
                                onValueChange = { model.glassBlurRadius(it.roundToInt()) },
                                valueRange = 0f..72f,
                                steps = 8,
                                enabled = prefs.glassMode in setOf("frosted", "liquid")
                            )
                            Text("Android 12+ also receives system window blur where the device supports it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun StudioSection27(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun StudioLink27(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (LocalNurGlass.current.enabled) 0.64f else 1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.11f)) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChoiceRow27(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            if (selected) Icon(Icons.Default.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
