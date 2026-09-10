package com.nshd.nurm3.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.nshd.nurm3.data.NurPreferences
import java.time.LocalDate
import kotlin.math.roundToInt

private data class StudioVisualChoice27(val id: String, val label: String, val icon: ImageVector)

private val studioThemeChoices27 = listOf(
    StudioVisualChoice27("system", "Follow system", Icons.Default.SettingsBrightness),
    StudioVisualChoice27("light", "Light", Icons.Default.LightMode),
    StudioVisualChoice27("dark", "Dark", Icons.Default.DarkMode),
    StudioVisualChoice27("amoled", "AMOLED", Icons.Default.Contrast)
)

private val studioProgressChoices27 = listOf(
    "slim" to "Slim",
    "thick" to "Thick",
    "wavy" to "Wavy",
    "squiggly" to "Squiggly"
)

/**
 * Appearance Studio after the solid-surface rollback.
 * Font settings remain under Text & typography; glass controls are intentionally removed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceStudio27(
    prefs: NurPreferences,
    model: NurViewModel,
    refreshStatus: String,
    navigate: (String) -> Unit
) {
    var picker by rememberSaveable { mutableStateOf("") }
    var previewDone by rememberSaveable { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(NurDesign.pagePadding),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            NurPageHeading(
                "Personalize",
                "Appearance Studio",
                "Tune color, typography, navigation and motion without changing your data."
            )
        }

        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Text(
                    "LIVE PREVIEW",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "نُور",
                    style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Serif),
                    color = NurDesign.gold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "A little light, every day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                NurLinearProgress(0.68f, LocalDate.MIN, "Appearance preview", style = prefs.progressStyle)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Checkbox(checked = previewDone, onCheckedChange = { previewDone = it })
                    Text(
                        if (previewDone) "Preview complete" else "Tap to preview interaction",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        item {
            StudioSection27("Theme & colors") {
                StudioLink27(
                    Icons.Default.Palette,
                    "Theme mode",
                    studioThemeChoices27.firstOrNull { it.id == prefs.themeMode }?.label ?: "Dark"
                ) { picker = "theme" }
                StudioLink27(
                    Icons.Default.ColorLens,
                    "Color palette",
                    if (prefs.dynamicColor) "Dynamic wallpaper colors" else prefs.palette.replaceFirstChar { it.uppercase() }
                ) { picker = "palette" }
                NurSettingRow(
                    "Dynamic colors",
                    "Use wallpaper colors on Android 12 or newer",
                    prefs.dynamicColor,
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                ) { model.setting("dynamic", it) }
            }
        }

        item {
            StudioSection27("Text & typography") {
                NurFontSettingsLink(prefs) { navigate("fonts") }
                NurPanel(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Typography size",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${(prefs.typeScale * 100).roundToInt()}%",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Slider(
                        value = prefs.typeScale,
                        onValueChange = model::typeScale,
                        valueRange = 0.85f..1.2f,
                        steps = 6
                    )
                    Text(
                        "Scales NUR text while keeping the interface responsive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                NurSettingRow(
                    "Arabic calligraphy",
                    "Show Arabic text in Daily Light and spiritual details",
                    prefs.showArabic
                ) { model.setting("arabic", it) }
            }
        }

        item {
            StudioSection27("Progress & motion") {
                StudioLink27(
                    Icons.Default.Timeline,
                    "Progression style",
                    studioProgressChoices27.firstOrNull { it.first == prefs.progressStyle }?.second ?: "Slim"
                ) { picker = "progress" }
                NurSettingRow(
                    "Reduce motion",
                    "Minimize decorative transitions and animations",
                    prefs.reduceMotion
                ) { model.setting("motion", it) }
            }
        }

        item {
            StudioSection27("Display") {
                StudioLink27(
                    Icons.Default.ViewWeek,
                    "Bottom navigation",
                    resolveNurTabs(prefs.bottomNavigation).joinToString(" · ") { it.label }
                ) { navigate("navigation") }
                NurSettingRow(
                    "Maximize refresh rate",
                    "Request the highest compatible display mode up to 120 Hz",
                    prefs.highRefreshRate
                ) { model.setting("high_refresh_rate", it) }
                Text(
                    refreshStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Android may choose a lower rate for battery, heat or system policy. This setting only affects NUR while it is open.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                NurSettingRow(
                    "Compact cards",
                    "Fit more information on smaller screens",
                    prefs.compactCards
                ) { model.setting("compact", it) }
                NurSettingRow(
                    "Soft shapes",
                    "Switch between rounded and precise corners",
                    prefs.softShapes
                ) { model.setting("shapes", it) }
            }
        }

        item {
            OutlinedButton(
                onClick = { confirmReset = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.RestartAlt, null)
                Spacer(Modifier.width(8.dp))
                Text("Reset appearance")
            }
        }
        item { Spacer(Modifier.height(10.dp)) }
    }

    when (picker) {
        "theme" -> StudioPicker27("Theme mode", onDismiss = { picker = "" }) {
            Text(
                "Choose how NUR follows your display. AMOLED uses true black backgrounds and surfaces.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    studioThemeChoices27.take(2).forEach { option ->
                        StudioThemeTile27(
                            option,
                            prefs.themeMode == option.id,
                            onClick = { model.choice("mode", option.id); picker = "" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    studioThemeChoices27.drop(2).forEach { option ->
                        StudioThemeTile27(
                            option,
                            prefs.themeMode == option.id,
                            onClick = { model.choice("mode", option.id); picker = "" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        "progress" -> StudioPicker27("Progression style", onDismiss = { picker = "" }) {
            Text(
                "A visual choice only. All styles use the same saved progress values.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                studioProgressChoices27.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { (id, label) ->
                            val selected = prefs.progressStyle == id
                            Surface(
                                onClick = { model.choice("progress_style", id); picker = "" },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.large,
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                border = BorderStroke(
                                    if (selected) 2.dp else 1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Column(
                                    Modifier.padding(12.dp).heightIn(min = 126.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    NurLinearProgress(0.68f, LocalDate.MIN, "$label preview", style = id)
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (selected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        "palette" -> StudioPicker27("Color palette", onDismiss = { picker = "" }) {
            Text(
                "Choose a signature accent, or enable Dynamic colors to use your wallpaper.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            listOf(
                Triple("gold", "Signature gold", Color(0xFFD9B96F)),
                Triple("ocean", "Ocean blue", Color(0xFF638FA9)),
                Triple("sage", "Sage green", Color(0xFF71977D)),
                Triple("rose", "Soft rose", Color(0xFFB98091))
            ).forEach { (id, title, swatch) ->
                StudioPaletteOption27(
                    title = title,
                    swatch = swatch,
                    selected = prefs.palette == id && !prefs.dynamicColor,
                    onClick = {
                        model.choice("palette", id)
                        model.setting("dynamic", false)
                        picker = ""
                    }
                )
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset appearance?") },
            text = { Text("Restore default theme, progression and Journey layout. Your entries, Dhikr counts and history will not change.") },
            confirmButton = {
                TextButton(onClick = {
                    model.resetAppearance()
                    confirmReset = false
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StudioSection27(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        content()
    }
}

@Composable
private fun StudioLink27(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = scheme.surface,
        contentColor = scheme.onSurface,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.65f))
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(shape = MaterialTheme.shapes.medium, color = scheme.primary.copy(alpha = 0.10f)) {
                Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = scheme.primary)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface
                )
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StudioThemeTile27(
    option: StudioVisualChoice27,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 132.dp),
        shape = MaterialTheme.shapes.large,
        color = if (selected) scheme.primaryContainer else scheme.surface,
        contentColor = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant)
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                option.icon,
                null,
                modifier = Modifier.size(28.dp),
                tint = if (selected) scheme.primary else scheme.onSurfaceVariant
            )
            Text(
                option.label,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) scheme.onPrimaryContainer else scheme.onSurface
            )
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    "Selected",
                    tint = scheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun StudioPaletteOption27(
    title: String,
    swatch: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) scheme.primaryContainer else scheme.surface,
        contentColor = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
        border = BorderStroke(1.dp, if (selected) scheme.primary else scheme.outlineVariant)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = swatch, shape = MaterialTheme.shapes.small, modifier = Modifier.size(30.dp)) { }
            Text(
                title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) scheme.onPrimaryContainer else scheme.onSurface
            )
            if (selected) Icon(Icons.Default.CheckCircle, "Selected", tint = scheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudioPicker27(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("Cancel") }
            Spacer(Modifier.height(12.dp))
        }
    }
}
