package com.nshd.nurm3.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.data.*
import java.time.LocalDate

@Composable
fun DailyJourneyScreen(entries: List<Entry>, completions: List<Completion>, today: LocalDate, prefs: NurPreferences, model: NurViewModel, navigate: (String) -> Unit) {
    val active = entries.filter { EntrySchedule.isActive(it, today) }
    val done = DailyProgress.completedIds(completions, today)
    val summary = DailyProgress.lightSummary(entries, completions, today)
    val pending by model.completionPending.collectAsStateWithLifecycle()
    val prayerEntries = active.filter { it.kind == NurKind.PRAYER }
    val prayersDone = prayerEntries.count { it.id in done }
    LazyColumn(contentPadding = PaddingValues(NurDesign.pagePadding), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            NurPageHeading("Your space", "Your Daily Journey", "A meaningful day, one step at a time.", action = {
                IconButton(onClick = { navigate("layout") }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = "Customize Journey")
                }
            })
        }
        items(prefs.journey.visible(), key = { it }) { id ->
            when (id) {
                JourneyCard.LIGHT -> DailyLightCard(summary, prayersDone, today, prefs)
                JourneyCard.PRAYERS -> SectionCard("Your prayers", "$prayersDone of 5 completed", null) {
                    if (prayerEntries.isEmpty()) Text("Loading your prayer checklist…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    prayerEntries.forEach { entry ->
                        ChecklistRow(entry, entry.id in done, { model.complete(entry.id, today, it) }, date = today, pending = CompletionGate.key(entry.id, today) in pending)
                    }
                }
                JourneyCard.AMANAH, JourneyCard.MUHASABA, JourneyCard.RHYTHM -> {
                    val section = active.filter { it.kind == id }
                    val count = section.count { it.id in done }
                    SectionCard(JourneyCard.titles[id] ?: id, "$count of ${section.size} completed", { navigate(id) }) {
                        if (section.isEmpty()) Text("Nothing scheduled today. Your saved entries are still available.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else {
                            NurLinearProgress(count.toFloat() / section.size, today, "${JourneyCard.titles[id] ?: id} progress")
                            section.take(3).forEach { entry ->
                                ChecklistRow(entry, entry.id in done, { model.complete(entry.id, today, it) }, date = today, pending = CompletionGate.key(entry.id, today) in pending)
                            }
                            if (section.size > 3) TextButton(onClick = { navigate(id) }) { Text("View all ${section.size} entries") }
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { navigate("dhikr") }, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Default.TouchApp, null, Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text("Dhikr")
                }
                OutlinedButton(onClick = { navigate("reflections") }, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Default.MenuBook, null, Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text("Quran")
                }
            }
        }
        item { DailyReflectionCard(today) { id -> navigate("reflections?verse=$id") } }
    }
}

@Composable
private fun DailyLightCard(summary: ProgressSummary, prayersDone: Int, today: LocalDate, prefs: NurPreferences) {
    val target = NurMotion.fraction(summary.fraction)
    val progress = rememberNurProgress(target, today, "Daily Light")
    val scheme = MaterialTheme.colorScheme
    NurPanel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("DAILY LIGHT", style = MaterialTheme.typography.labelSmall, color = scheme.primary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("A little light, every day", style = MaterialTheme.typography.titleLarge)
            }
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(24.dp))
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(206.dp).clearAndSetSemantics {
                contentDescription = "Daily Light: ${summary.completed} of ${summary.total} completed. $prayersDone of 5 prayers."
                progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f)
            }, contentAlignment = Alignment.Center) {
                NurCircularProgress(prayersDone / 5f, today, "Prayer ring", modifier = Modifier.size(194.dp), strokeWidth = 3.dp, color = scheme.primary.copy(alpha = 0.7f), trackColor = scheme.surfaceVariant.copy(alpha = 0.45f))
                NurCircularProgress(target, today, "Daily Light ring", modifier = Modifier.size(158.dp), strokeWidth = 10.dp, color = scheme.primary, trackColor = scheme.surfaceVariant.copy(alpha = 0.65f))
                listOf(Alignment.TopCenter, Alignment.BottomCenter, Alignment.CenterStart, Alignment.CenterEnd).forEach { alignment ->
                    Box(Modifier.fillMaxSize(), contentAlignment = alignment) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(12.dp))
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("الله", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = scheme.primary)
                    Text("${NurMotion.percent(progress)}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Today's progress", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            Text("${summary.completed} / ${summary.total}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        NurLinearProgress(target, today, "Daily Light total progress")
        Text("$prayersDone of 5 prayers completed", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
        if (prefs.showArabic) {
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.55f))
            Text("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleSmall, color = scheme.primary)
        }
    }
}

@Composable
fun SectionCard(title: String, subtitle: String, onOpen: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    val reduceMotion = LocalNurReduceMotion.current
    NurPanel(Modifier.fillMaxWidth().then(if (reduceMotion) Modifier else Modifier.animateContentSize())) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onOpen != null) IconButton(onClick = onOpen, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Open $title", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        content()
    }
}
