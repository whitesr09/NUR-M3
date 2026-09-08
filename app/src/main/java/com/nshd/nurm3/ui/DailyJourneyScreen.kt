package com.nshd.nurm3.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
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
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.data.*
import java.time.LocalDate

@Composable
fun DailyJourneyScreen(entries: List<Entry>, completions: List<Completion>, today: LocalDate, prefs: NurPreferences, model: NurViewModel, navigate: (String) -> Unit) {
    val active = entries.filter { EntrySchedule.isActive(it, today) }
    val done = DailyProgress.completedIds(completions, today)
    val summary = DailyProgress.summary(entries, completions, today)
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Your Daily Journey", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Text("A meaningful day, one step at a time.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { navigate("layout") }) { Icon(Icons.Default.Tune, contentDescription = "Customize Journey") }
            }
        }
        items(prefs.journey.visible(), key = { it }) { id ->
            when (id) {
                JourneyCard.LIGHT -> DailyLightCard(summary, active.filter { it.kind == NurKind.PRAYER }.count { it.id in done }, prefs)
                JourneyCard.PRAYERS -> {
                    SectionCard("Your prayers", "Five daily prayers", null) {
                        active.filter { it.kind == NurKind.PRAYER }.forEach { entry ->
                            ChecklistRow(entry, entry.id in done, { model.complete(entry.id, today, it) })
                        }
                    }
                }
                JourneyCard.AMANAH, JourneyCard.MUHASABA, JourneyCard.RHYTHM -> {
                    val kind = id
                    val section = active.filter { it.kind == kind }
                    val count = section.count { it.id in done }
                    SectionCard(JourneyCard.titles[id] ?: id, "$count of ${section.size} completed", { navigate(id) }) {
                        if (section.isEmpty()) Text("No items scheduled today.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else {
                            LinearProgressIndicator(progress = { count.toFloat() / section.size }, modifier = Modifier.fillMaxWidth())
                            section.take(3).forEach { entry -> ChecklistRow(entry, entry.id in done, { model.complete(entry.id, today, it) }) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyLightCard(summary: ProgressSummary, prayersDone: Int, prefs: NurPreferences) {
    val target = summary.fraction.coerceIn(0f, 1f)
    val progress by animateFloatAsState(targetValue = target, animationSpec = if (prefs.reduceMotion) snap() else androidx.compose.animation.core.tween(650), label = "Daily Light progress")
    val outer by animateFloatAsState(targetValue = prayersDone / 5f, animationSpec = if (prefs.reduceMotion) snap() else androidx.compose.animation.core.tween(650), label = "Prayer progress")
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(if (LocalNurCompact.current) 16.dp else 22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (prefs.showArabic) Text("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text("Daily Light", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Box(Modifier.size(198.dp).semantics(mergeDescendants = true) { contentDescription = "Daily Light: ${summary.completed} of ${summary.total} completed"; progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f) }, contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { outer }, modifier = Modifier.size(188.dp), strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f), trackColor = MaterialTheme.colorScheme.surfaceVariant)
                CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(154.dp), strokeWidth = 10.dp, color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                listOf(Alignment.TopCenter, Alignment.BottomCenter, Alignment.CenterStart, Alignment.CenterEnd).forEach { alignment ->
                    Box(Modifier.fillMaxSize(), contentAlignment = alignment) { Text("✦", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium) }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("الله", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                    Text("${(target * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            Text("${summary.completed} of ${summary.total} completed", style = MaterialTheme.typography.bodyMedium)
            Text("$prayersDone of 5 prayers", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            Text("Your journey is built one sincere action at a time.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionCard(title: String, subtitle: String, onOpen: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(if (LocalNurCompact.current) 12.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (onOpen != null) IconButton(onClick = onOpen) { Icon(Icons.Default.ChevronRight, contentDescription = "Open $title") }
            }
            content()
        }
    }
}

@Composable
fun ChecklistRow(entry: Entry, checked: Boolean, onChecked: (Boolean) -> Unit, enabled: Boolean = true, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChecked, enabled = enabled)
        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(entry.title, style = MaterialTheme.typography.bodyLarge)
            if (entry.kind != NurKind.PRAYER && entry.schedule != "daily") Text(entry.schedule.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}
