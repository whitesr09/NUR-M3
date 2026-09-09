package com.nshd.nurm3.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nshd.nurm3.data.NurBottomNavigationChoices
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Routes that can be pinned into NUR's five-slot bottom navigation. */
data class NurNavItem(val route: String, val label: String, val icon: ImageVector)

val NurAvailableTabs = listOf(
    NurNavItem("journey", "Today", Icons.Default.Home),
    NurNavItem("amanah", "Amanah", Icons.Default.Checklist),
    NurNavItem("focus", "Focus", Icons.Default.Timer),
    NurNavItem("muhasaba", "Reflect", Icons.Default.EditNote),
    NurNavItem("rhythm", "Rhythm", Icons.Default.Repeat),
    NurNavItem("history", "History", Icons.Default.History)
)

val NurMainTabs: List<NurNavItem> = resolveNurTabs(NurBottomNavigationChoices.defaults)

fun resolveNurTabs(routes: List<String>): List<NurNavItem> {
    val byRoute = NurAvailableTabs.associateBy { it.route }
    return NurBottomNavigationChoices.normalize(routes.drop(1).joinToString(",")).mapNotNull(byRoute::get)
}

private val routeTitles = mapOf(
    "settings" to "Settings",
    "appearance" to "Appearance Studio",
    "fonts" to "Font style",
    "navigation" to "Bottom navigation",
    "layout" to "Customize Journey",
    "insights" to "Insights",
    "backup" to "Backup & restore",
    "secure-backup" to "Encrypted backup",
    "backup-health" to "Backup health",
    "privacy" to "Privacy",
    "dhikr" to "Dhikr",
    "focus" to "Focus & Routine",
    "ai" to "NUR AI",
    "widgets" to "Widgets",
    "accessibility" to "Accessibility & language",
    "reflections" to "Quran Reflections",
    "reflections?verse={verse}" to "Quran Reflections"
)

@Composable
fun NurTopBar(
    route: String,
    today: LocalDate,
    tabs: List<NurNavItem> = NurMainTabs,
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
    val home = route == "journey"
    val auxiliary = route !in tabs.map { it.route }
    Box(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        NurGlassChromeSurface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (auxiliary) {
                        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            routeTitles[route] ?: "NUR",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            "نُور",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 31.sp, fontFamily = FontFamily.Serif),
                            fontWeight = FontWeight.Bold,
                            color = NurDesign.gold
                        )
                        if (home) {
                            Text(
                                today.format(DateTimeFormatter.ofPattern("d MMM yyyy")),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                tabs.firstOrNull { it.route == route }?.label ?: "NUR",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (!auxiliary) {
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Settings, contentDescription = "Open settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (home) {
                    Text(
                        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 9.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
fun NurBottomBar(
    current: String,
    tabs: List<NurNavItem> = NurMainTabs,
    onNavigate: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val reduce = LocalNurReduceMotion.current
    Box(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 10.dp, end = 10.dp, bottom = 8.dp)
    ) {
        NurGlassChromeSurface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(34.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                tabs.forEach { item ->
                    val selected = current == item.route
                    val tint by animateColorAsState(
                        if (selected) scheme.primary else scheme.onSurfaceVariant,
                        animationSpec = if (reduce) snap() else tween(180),
                        label = "Navigation tint"
                    )
                    val interaction = remember { MutableInteractionSource() }
                    Column(
                        Modifier.weight(1f).heightIn(min = 58.dp).clip(RoundedCornerShape(22.dp))
                            .background(if (selected) scheme.primary.copy(alpha = 0.14f) else Color.Transparent)
                            .clickable(interactionSource = interaction, indication = ripple(), role = Role.Tab) { onNavigate(item.route) }
                            .semantics { this.selected = selected },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(item.icon, contentDescription = null, modifier = Modifier.size(21.dp), tint = tint)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            item.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = tint,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
