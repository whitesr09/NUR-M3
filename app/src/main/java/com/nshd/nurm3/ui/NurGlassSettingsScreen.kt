package com.nshd.nurm3.ui

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.data.NurGlassSettingsStore
import kotlin.math.roundToInt

private val glassModes = listOf(
    "off" to "Off",
    "subtle" to "Subtle",
    "frosted" to "Frosted",
    "liquid" to "Liquid glass"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NurGlassSettingsScreen() {
    val context = LocalContext.current
    val store = remember(context) { NurGlassSettingsStore.get(context) }
    val config by store.settings.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(NurDesign.pagePadding),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            NurPageHeading(
                "Appearance",
                "Glass & depth",
                "Choose how strongly NUR uses translucent surfaces, depth, blur and liquid highlights."
            )
        }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Live preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Text("Daily Light", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text("Glass surfaces now apply across NUR panels instead of being a settings-only mockup.", style = MaterialTheme.typography.bodyMedium)
                NurLinearProgress(0.72f, java.time.LocalDate.MIN, "Glass preview")
            }
        }
        item {
            Text("Surface style", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                glassModes.forEach { (id, label) ->
                    FilterChip(
                        selected = config.mode == id,
                        onClick = { store.setMode(id) },
                        label = { Text(label) },
                        leadingIcon = if (config.mode == id) ({ Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp)) }) else null
                    )
                }
            }
        }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.BlurOn, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Glass intensity", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.weight(1f))
                    Text("${(config.intensity * 100).roundToInt()}%", color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = config.intensity,
                    onValueChange = { store.setIntensity(it) },
                    valueRange = 0.15f..0.85f,
                    enabled = config.mode != "off"
                )
                Text("Controls translucency, highlight strength and visible depth.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Corner fluidity", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.weight(1f))
                    Text("${config.radius} dp", color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = config.radius.toFloat(),
                    onValueChange = { store.setRadius(it.roundToInt()) },
                    valueRange = 8f..32f,
                    steps = 11,
                    enabled = config.mode != "off"
                )
                Text("Higher values produce softer, more liquid card geometry.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Text("Blur behavior", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        "Android 12+ can also use system window blur where supported. NUR still renders its own translucent depth so the theme remains visible when OEM blur is unavailable."
                    else
                        "This Android version uses NUR's own translucent depth and blurred decorative backdrop. System window blur requires Android 12 or newer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            OutlinedButton(onClick = { store.reset() }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Reset glass appearance")
            }
        }
    }
}
