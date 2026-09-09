package com.nshd.nurm3.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.data.NurPreferences

/** A presentation editor. Every command changes DataStore, never Room records. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JourneyStudio14(prefs: NurPreferences, model: NurViewModel) {
    val layout = prefs.journey
    val options = prefs.journeyOptions
    var showSave by rememberSaveable { mutableStateOf(false) }
    var presetName by rememberSaveable { mutableStateOf("") }
    var confirmReset by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(NurDesign.pagePadding), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { NurPageHeading("Your dashboard", "Journey Studio", "Build a home screen around what matters to you.") }
        item {
            NurPanel {
                Text("LAYOUT PREVIEW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                options.visible(layout).forEach { id ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(if (id in options.pinned) Icons.Default.PushPin else Icons.Default.DragIndicator, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(JourneyCard.titles[id] ?: id, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(if (id in options.collapsed) "Folded" else options.size(id).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("The preview shows your saved card order and sizes. It does not create or change activity.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Text("Layout presets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NurChoicePill("Balanced", layout == JourneyLayout.DEFAULT && options == JourneyOptions(), { model.applyJourneyLayout(JourneyLayout.DEFAULT) })
                NurChoicePill("Prayer first", layout == JourneyLayout.PRAYER_FIRST, { model.applyJourneyLayout(JourneyLayout.PRAYER_FIRST) })
                NurChoicePill("Focus", layout == JourneyLayout.FOCUS, { model.applyJourneyLayout(JourneyLayout.FOCUS) })
                prefs.savedJourneyPreset?.let { saved ->
                    NurChoicePill(saved.name, layout == saved.layout && options == saved.options, { model.applyJourneyPreset() })
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { presetName = prefs.savedJourneyPreset?.name.orEmpty(); showSave = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Default.BookmarkAdd, null); Spacer(Modifier.width(8.dp)); Text("Save current layout as preset")
            }
            Text("You can save one named personal preset. Saving again replaces that preset only; it does not change your entries.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Text("Your cards", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("Pin favorites, choose a size, fold sections or reorder them. All options are saved automatically.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(layout.order, key = { it }) { id ->
            val title = JourneyCard.titles[id] ?: id
            val visible = id !in layout.hidden
            val index = layout.order.indexOf(id)
            val pinned = id in options.pinned
            val collapsed = id in options.collapsed
            NurPanel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(when (id) {
                        JourneyCard.LIGHT -> Icons.Default.AutoAwesome
                        JourneyCard.PRAYERS -> Icons.Default.Mosque
                        JourneyCard.AMANAH -> Icons.Default.Checklist
                        JourneyCard.MUHASABA -> Icons.Default.EditNote
                        else -> Icons.Default.Repeat
                    }, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(if (visible) "Visible · ${options.size(id)}" else "Hidden", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = visible, onCheckedChange = { model.journey(layout.show(id, it)) }, enabled = !visible || layout.visible().size > 1)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("Card size", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    JourneySize.all.forEach { size ->
                        NurChoicePill(size.replaceFirstChar { it.uppercase() }, options.size(id) == size,
                            { model.journeyOptions { it.withSize(id, size) } }, enabled = visible)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = pinned, onClick = { model.journeyOptions { it.withPin(id, !pinned) } }, enabled = visible,
                        label = { Text("Pin top") }, leadingIcon = if (pinned) ({ Icon(Icons.Default.PushPin, null, Modifier.size(16.dp)) }) else null)
                    FilterChip(selected = collapsed, onClick = { model.journeyOptions { it.withCollapsed(id, !collapsed) } }, enabled = visible,
                        label = { Text("Folded") }, leadingIcon = if (collapsed) ({ Icon(Icons.Default.UnfoldMore, null, Modifier.size(16.dp)) }) else null)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Position ${index + 1} of ${layout.order.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row {
                        IconButton(onClick = { model.journey(layout.move(id, -1)) }, enabled = index > 0, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move $title up") }
                        IconButton(onClick = { model.journey(layout.move(id, 1)) }, enabled = index < layout.order.lastIndex, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move $title down") }
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Icon(Icons.Default.RestartAlt, null); Spacer(Modifier.width(8.dp)); Text("Restore default dashboard") }
            Text("At least one card remains visible. Hiding, resizing, pinning or folding a card never deletes its saved entries or completion history.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (showSave) AlertDialog(onDismissRequest = { showSave = false }, title = { Text("Save layout preset") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Name this layout so you can restore it later.")
            OutlinedTextField(value = presetName, onValueChange = { if (it.length <= 32) presetName = it }, singleLine = true, label = { Text("Preset name") }, modifier = Modifier.fillMaxWidth())
            if (prefs.savedJourneyPreset != null) Text("This replaces your previous personal preset.", style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { TextButton(onClick = { model.journeyPreset(presetName); showSave = false }, enabled = com.nshd.nurm3.ui.JourneyPreset.validName(presetName)) { Text("Save") } },
        dismissButton = { TextButton(onClick = { showSave = false }) { Text("Cancel") } })
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false }, title = { Text("Restore dashboard?") },
        text = { Text("Restore the original card order, visibility and sizes. Your saved personal preset and all activity records will remain.") },
        confirmButton = { TextButton(onClick = { model.applyJourneyLayout(JourneyLayout.DEFAULT); confirmReset = false }) { Text("Restore") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } })
}
