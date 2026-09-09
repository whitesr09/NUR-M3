package com.nshd.nurm3.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.data.NurPreferences
import kotlin.math.roundToInt

/** Keeps every existing Appearance Studio control while adding font and glass studios. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppearanceStudio14(prefs: NurPreferences, model: NurViewModel, refreshStatus: String, navigate: (String) -> Unit) {
    var glassSheet by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = NurDesign.pagePadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NurFontSettingsLink(prefs) { navigate("fonts") }
            NurPanel(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.BlurOn, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text("Glass & depth", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            when (prefs.glassMode) {
                                "liquid" -> "Liquid glass · ${prefs.glassBlurRadius}px blur"
                                "frosted" -> "Frosted glass · ${prefs.glassBlurRadius}px blur"
                                "subtle" -> "Subtle translucent surfaces"
                                else -> "Solid Material surfaces"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { glassSheet = true }) { Icon(Icons.Default.ChevronRight, "Open glass settings") }
                }
            }
        }
        Box(Modifier.weight(1f)) {
            AppearanceStudio(prefs, model, refreshStatus)
        }
    }

    if (glassSheet) ModalBottomSheet(onDismissRequest = { glassSheet = false }) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Glass & depth", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "Choose how strongly NUR uses translucent Material surfaces, blurred depth fields and Android window blur. Liquid mode adds a slow moving highlight unless Reduce motion is enabled.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple("off", "Off", Icons.Default.VisibilityOff),
                    Triple("subtle", "Subtle", Icons.Default.Opacity),
                    Triple("frosted", "Frosted", Icons.Default.BlurOn),
                    Triple("liquid", "Liquid", Icons.Default.WaterDrop)
                ).forEach { (id, label, icon) ->
                    FilterChip(
                        selected = prefs.glassMode == id,
                        onClick = { model.choice("glass_mode", id) },
                        label = { Text(label) },
                        leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) }
                    )
                }
            }
            NurPanel(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Glass intensity", style = MaterialTheme.typography.titleSmall)
                    Text("${(prefs.glassIntensity * 100).roundToInt()}%", color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = prefs.glassIntensity,
                    onValueChange = model::glassIntensity,
                    valueRange = 0.25f..1f,
                    enabled = prefs.glassMode != "off"
                )
                Text("Controls translucency, depth color and highlight strength.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text("On Android 12+ NUR also requests real window-background blur. Other versions still get the layered glass depth field.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = { glassSheet = false }, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            Spacer(Modifier.height(16.dp))
        }
    }
}
