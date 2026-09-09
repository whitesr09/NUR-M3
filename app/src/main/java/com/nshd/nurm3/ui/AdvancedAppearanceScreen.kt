package com.nshd.nurm3.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.data.AdvancedSettings
import com.nshd.nurm3.data.VisualPreferences
import java.time.LocalDate
import kotlinx.coroutines.launch

/** User-owned appearance choices; preview state never touches Room. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancedAppearanceScreen(model: NurViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings = remember(context) { AdvancedSettings(context.applicationContext) }
    val visual by settings.preferences.collectAsStateWithLifecycle(initialValue = VisualPreferences())
    val scope = rememberCoroutineScope()
    val styles = listOf("slim", "thick", "wavy", "squiggly")
    val modes = listOf("off" to "Off", "subtle" to "Subtle", "frosted" to "Frosted", "liquid" to "Liquid")
    var sample by remember { mutableFloatStateOf(0.65f) }
    var reset by remember { mutableStateOf(false) }
    val saveChoice: (String, String) -> Unit = { key, value -> scope.launch { settings.updateChoice(key, value) } }
    val saveFloat: (String, Float) -> Unit = { key, value -> scope.launch { settings.updateFloat(key, value) } }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { NurPageHeading("Appearance", "Surface & motion", "Customize glass, progress, and animation independently.") }
        item {
            CompositionLocalProvider(LocalNurGlass provides visual.glass) {
                NurGlassSurface(Modifier.fillMaxWidth()) {
                    Text("LIVE PREVIEW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text("Your Daily Journey", style = MaterialTheme.typography.titleLarge)
                    Text("A calm, readable glass surface", style = MaterialTheme.typography.bodyMedium)
                    NurLinearProgress(sample, LocalDate.MIN, "Preview bar", style = visual.barStyle)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        NurCircularProgress(sample, LocalDate.MIN, "Preview ring", modifier = Modifier.size(100.dp), style = visual.ringStyle)
                    }
                    Slider(value = sample, onValueChange = { sample = it }, valueRange = 0f..1f)
                    Text("Preview only. No task or prayer records are changed.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Text("Glass surface", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                modes.forEach { (id, title) -> NurChoicePill(title, visual.glass.mode == id, { saveChoice("glass_mode", id) }) }
            }
        }
        if (visual.glass.enabled) {
            item { AdvancedSlider("Glass intensity", visual.glass.intensity, 0.15f..0.85f, { saveFloat("glass_intensity", it) }) }
            item { AdvancedSlider("Corner radius", visual.glass.radius.toFloat(), 8f..32f, { scope.launch { settings.updateRadius(it.toInt()) } }, " dp") }
        }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Text("About Liquid Glass", style = MaterialTheme.typography.titleSmall)
                Text("Translucent layers, delicate highlights, and rounded edges create the glass effect. Ordinary cards do not capture or blur private content behind them. Real Android window blur is supported only for suitable windows on compatible devices. This mode remains optional and can be disabled for contrast or performance.", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Text("Ring style", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                styles.forEach { id -> NurChoicePill(id.replaceFirstChar { it.uppercase() }, visual.ringStyle == id, { saveChoice("ring_style", id) }) }
            }
        }
        item {
            Text("Bar style", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                styles.forEach { id -> NurChoicePill(id.replaceFirstChar { it.uppercase() }, visual.barStyle == id, { saveChoice("bar_style", id) }) }
            }
        }
        item { AdvancedSlider("Ring thickness", visual.ringStroke, 0.6f..1.8f, { saveFloat("ring_stroke", it) }, "×") }
        item { AdvancedSlider("Bar thickness", visual.barStroke, 0.6f..1.8f, { saveFloat("bar_stroke", it) }, "×") }
        item { AdvancedSlider("Motion intensity", visual.motionIntensity, 0f..1.5f, { saveFloat("motion_intensity", it) }, "×") }
        item { Text("System reduced-motion settings take priority. These controls change presentation only; progress remains based on saved records.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { OutlinedButton(onClick = { reset = true }, modifier = Modifier.fillMaxWidth()) { Text("Reset advanced appearance") } }
    }
    if (reset) AlertDialog(onDismissRequest = { reset = false }, title = { Text("Reset visual settings?") }, text = { Text("Glass, ring, bar, and motion settings will return to defaults. Your records and custom fonts are not affected.") }, confirmButton = { TextButton(onClick = { scope.launch { settings.reset() }; reset = false }) { Text("Reset") } }, dismissButton = { TextButton(onClick = { reset = false }) { Text("Cancel") } })
}

@Composable
private fun AdvancedSlider(title: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit, suffix: String = "") {
    NurPanel(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text("${(value * 100).toInt() / 100f}$suffix", color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range)
    }
}
