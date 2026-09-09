package com.nshd.nurm3.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.data.NurBottomNavigationChoices
import com.nshd.nurm3.data.NurPreferences

/** Four customizable slots plus the permanent Today destination. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationSettingsScreen(prefs: NurPreferences, model: NurViewModel) {
    var editingSlot by rememberSaveable { mutableIntStateOf(-1) }
    val current = prefs.bottomNavigation
    val selectedOptional = current.drop(1)

    Column(Modifier.fillMaxSize()) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(NurDesign.pagePadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                NurPageHeading(
                    "Navigation",
                    "Choose your five tabs",
                    "Today stays first. Pick the four tools you want one tap away from anywhere in NUR."
                )
            }
            item {
                NurPanel(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Today", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("Permanent first tab", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            items(4) { index ->
                val route = selectedOptional.getOrNull(index) ?: NurBottomNavigationChoices.defaults[index + 1]
                val item = NurAvailableTabs.first { it.route == route }
                Surface(
                    onClick = { editingSlot = index },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = if (LocalNurGlass.current.enabled) 0.58f else 1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.11f)) {
                            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                Icon(item.icon, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Slot ${index + 2} · ${item.label}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("Tap to replace", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { model.bottomNavigation(NurBottomNavigationChoices.defaults) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reset to Today · Amanah · Focus · Rhythm · History")
                }
            }
            item {
                Text(
                    "Changing the navigation bar never deletes a feature. Removed tabs remain available from Settings and the dashboard shortcuts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (editingSlot in 0..3) {
        ModalBottomSheet(onDismissRequest = { editingSlot = -1 }) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Choose tab ${editingSlot + 2}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text("A destination already used in another slot is disabled.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                NurAvailableTabs.filter { it.route != "journey" }.forEach { candidate ->
                    val slotRoute = selectedOptional.getOrNull(editingSlot)
                    val usedElsewhere = candidate.route in selectedOptional && candidate.route != slotRoute
                    Surface(
                        onClick = {
                            if (!usedElsewhere) {
                                val next = current.toMutableList()
                                while (next.size < 5) next += NurBottomNavigationChoices.defaults[next.size]
                                next[editingSlot + 1] = candidate.route
                                model.bottomNavigation(next)
                                editingSlot = -1
                            }
                        },
                        enabled = !usedElsewhere,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = if (candidate.route == slotRoute) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(candidate.icon, null, tint = if (usedElsewhere) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary)
                            Text(candidate.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            if (candidate.route == slotRoute) Icon(Icons.Default.Check, "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
