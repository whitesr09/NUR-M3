package com.nshd.nurm3.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun SettingsHub(navigate: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Make NUR yours", style = MaterialTheme.typography.headlineMedium)
            Text("Personalize your experience and manage your information.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { HubLink(Icons.Default.Palette, "Appearance Studio", "Colors, typography, shapes and motion") { navigate("appearance") } }
        item { HubLink(Icons.Default.ViewAgenda, "Daily Journey", "Arrange and customize your home screen") { navigate("layout") } }
        item { HubLink(Icons.Default.Backup, "Backup & restore", "Export and safely import your saved data") { navigate("backup") } }
        item { HubLink(Icons.Default.Insights, "Insights", "Recorded activity and habit consistency") { navigate("insights") } }
        item {
            HorizontalDivider()
            Text("NUR Material 3", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            Text("Development build • Made by NSHD", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Your records stay on this device unless you explicitly export them. The original NUR and early NUR-M3 installs are not automatically migrated.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HubLink(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}
