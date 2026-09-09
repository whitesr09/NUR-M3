package com.nshd.nurm3.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalMotionDurationScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Stable routes retain the existing saved navigation state. */
data class NurNavItem(val route: String, val label: String, val icon: ImageVector)
val NurMainTabs = listOf(
    NurNavItem("journey", "Today", Icons.Default.Home),
    NurNavItem("amanah", "Amanah", Icons.Default.Checklist),
    NurNavItem("muhasaba", "Reflect", Icons.Default.EditNote),
    NurNavItem("rhythm", "Rhythm", Icons.Default.Repeat),
    NurNavItem("history", "History", Icons.Default.History)
)

private val routeTitles = mapOf(
    "settings" to "Settings", "appearance" to "Appearance Studio", "layout" to "Customize Journey",
    "insights" to "Insights", "backup" to "Backup & restore", "privacy" to "Privacy",
    "dhikr" to "Dhikr", "reflections?verse={verse}" to "Quran Reflections"
)

@Composable
fun NurTopBar(route: String, today: LocalDate, onBack: () -> Unit, onSettings: () -> Unit) {
    val home = route == "journey"
    val auxiliary = route !in NurMainTabs.map { it.route }
    Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
                if (auxiliary) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(routeTitles[route] ?: "NUR", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                } else {
                    Text("نُور", modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium.copy(fontSize = 31.sp, fontFamily = FontFamily.Serif), fontWeight = FontWeight.Bold, color = NurDesign.gold)
                    if (home) Text(today.format(DateTimeFormatter.ofPattern("d MMM yyyy")), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else Text(NurMainTabs.firstOrNull { it.route == route }?.label ?: "NUR", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!auxiliary) {
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = "Open settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (home) {
                Text("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, maxLines = 2)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        }
    }
}

@Composable
fun NurBottomBar(current: String, onNavigate: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val reduce = LocalNurReduceMotion.current || LocalMotionDurationScale.current.scaleFactor <= 0f
    Surface(color = scheme.background, tonalElevation = 0.dp) {
        Column {
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.65f))
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                NurMainTabs.forEach { item ->
                    val selected = current == item.route
                    val tint by animateColorAsState(if (selected) scheme.primary else scheme.onSurfaceVariant, animationSpec = if (reduce) snap() else tween(180), label = "Navigation tint")
                    val interaction = remember { MutableInteractionSource() }
                    Column(
                        Modifier.weight(1f).heightIn(min = 58.dp).clip(MaterialTheme.shapes.medium)
                            .background(if (selected) scheme.primary.copy(alpha = 0.10f) else Color.Transparent)
                            .clickable(interactionSource = interaction, indication = ripple(), role = Role.Tab) { onNavigate(item.route) }
                            .semantics { this.selected = selected },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        Icon(item.icon, contentDescription = null, modifier = Modifier.size(21.dp), tint = tint)
                        Spacer(Modifier.height(5.dp))
                        Text(item.label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = tint, maxLines = 1, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
