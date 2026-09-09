package com.nshd.nurm3.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Allows feature screens to offer Undo through the root SnackbarHost. */
val LocalNurSnackbarHost = staticCompositionLocalOf<SnackbarHostState?> { null }

@Composable
fun NurChoicePill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val shape = RoundedCornerShape(12.dp)
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier.heightIn(min = 44.dp).clip(shape)
            .background(if (selected) scheme.primary.copy(alpha = 0.12f) else scheme.surface)
            .border(1.dp, if (selected) scheme.primary.copy(alpha = 0.7f) else scheme.outlineVariant, shape)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, interactionSource = interaction, indication = ripple(), onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = if (selected) scheme.primary else scheme.onSurfaceVariant)
    }
}

@Composable
fun NurEmptyState(icon: ImageVector, title: String, message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    NurPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(64.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            if (actionLabel != null && onAction != null) NurPrimaryAction(actionLabel, onAction, modifier = Modifier.fillMaxWidth(), icon = { Icon(Icons.Default.Add, null, Modifier.size(18.dp)) })
        }
    }
}
