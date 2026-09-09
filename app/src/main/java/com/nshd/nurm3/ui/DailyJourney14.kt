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
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** The home screen consumes existing Room state; presentation preferences never alter progress. */
@Composable
fun DailyJourneyScreen14(entries: List<Entry>, completions: List<Completion>, today: LocalDate, prefs: NurPreferences, model: NurViewModel, navigate: (String) -> Unit) {
    val active = remember(entries, today) { entries.filter { EntrySchedule.isActive(it, today) } }
    val done = DailyProgress.completedIds(completions, today)
    val summary = DailyProgress.lightSummary(entries, completions, today)
    val pending by model.completionPending.collectAsStateWithLifecycle()
    val prayers = active.filter { it.kind == NurKind.PRAYER }
    val prayersDone = prayers.count { it.id in done }
    val options = prefs.journeyOptions
    val nextPrayer = prayers.firstOrNull { it.id !in done }
    val nextTask = active.firstOrNull { it.kind == NurKind.AMANAH && it.id !in done }
    val next = nextTask ?: nextPrayer ?: active.firstOrNull { it.kind != NurKind.PRAYER && it.id !in done }
    val greeting = when (LocalTime.now().hour) { in 5..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" }
    LazyColumn(contentPadding = PaddingValues(NurDesign.pagePadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            NurPageHeading("$greeting · ${today.format(DateTimeFormatter.ofPattern("EEE, d MMM"))}", "Your Daily Journey", "A meaningful day, one step at a time.", action = {
                IconButton(onClick = { navigate("layout") }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Tune, contentDescription = "Customize Journey") }
            })
        }
        item {
            NurPanel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.WbTwilight, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text("Your next step", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(if (nextPrayer == null) "All five prayers checked" else "Next unchecked prayer: ${nextPrayer.title}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (next != null) {
                    Text(next.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(if (next.kind == NurKind.PRAYER) "Prayer checklist · no calculated prayer time" else "Scheduled for today · ${next.kind.replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val saving = CompletionGate.key(next.id, today) in pending
                        NurPrimaryAction(if (saving) "Saving…" else "Mark complete", { model.complete(next.id, today, true) }, modifier = Modifier.weight(1f), enabled = !saving && today == LocalDate.now(), icon = { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) })
                        OutlinedButton(onClick = { navigate(if (next.kind == NurKind.PRAYER) "journey" else next.kind) }, modifier = Modifier.heightIn(min = 52.dp)) { Text("Open") }
                    }
                } else Text("Everything scheduled for today is complete. Your saved entries remain available tomorrow.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JourneyQuickAction("Tasks", Icons.Default.AddTask, Modifier.weight(1f)) { navigate("amanah") }
                JourneyQuickAction("Dhikr", Icons.Default.TouchApp, Modifier.weight(1f)) { navigate("dhikr") }
                JourneyQuickAction("Quran", Icons.Default.MenuBook, Modifier.weight(1f)) { navigate("reflections") }
                JourneyQuickAction("Insights", Icons.Default.Insights, Modifier.weight(1f)) { navigate("insights") }
            }
        }
        items(options.visible(prefs.journey), key = { it }) { id ->
            val size = options.size(id)
            val collapsed = id in options.collapsed
            val section = active.filter { it.kind == id }
            val count = section.count { it.id in done }
            if (id == JourneyCard.LIGHT) {
                JourneyLight14(summary, prayersDone, today, prefs, size, collapsed, { model.journeyOptions { it.withCollapsed(id, !collapsed) } })
            } else {
                val items = if (id == JourneyCard.PRAYERS) prayers else section
                val completed = if (id == JourneyCard.PRAYERS) prayersDone else count
                JourneySection14(id, items, completed, done, pending, today, size, collapsed,
                    onFold = { model.journeyOptions { it.withCollapsed(id, !collapsed) } },
                    onOpen = { navigate(if (id == JourneyCard.PRAYERS) "journey" else id) },
                    onCheck = { entry, checked -> model.complete(entry.id, today, checked) })
            }
        }
        item { DailyReflectionCard(today) { id -> navigate("reflections?verse=$id") } }
        item { Text("Your cards, goals and history stay saved when the day changes. Only each day's completion state is refreshed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun JourneyQuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.heightIn(min = 76.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(5.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
private fun JourneySection14(id: String, entries: List<Entry>, count: Int, done: Set<String>, pending: Set<String>, today: LocalDate, size: String, collapsed: Boolean, onFold: () -> Unit, onOpen: () -> Unit, onCheck: (Entry, Boolean) -> Unit) {
    val title = JourneyCard.titles[id] ?: id
    val fraction = if (entries.isEmpty()) 0f else count.toFloat() / entries.size
    val reduce = LocalNurReduceMotion.current
    NurPanel(Modifier.fillMaxWidth().then(if (reduce) Modifier else Modifier.animateContentSize())) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("$count of ${entries.size} completed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (id != JourneyCard.PRAYERS) IconButton(onClick = onOpen, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.OpenInNew, contentDescription = "Open $title") }
            IconButton(onClick = onFold, modifier = Modifier.size(48.dp)) { Icon(if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess, contentDescription = if (collapsed) "Expand $title" else "Fold $title") }
        }
        if (!collapsed) {
            NurLinearProgress(fraction, today, "$title progress")
            if (entries.isEmpty()) Text("Nothing scheduled today. Your saved entries are still available.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else if (size != JourneySize.COMPACT) {
                val limit = if (size == JourneySize.EXPANDED) 8 else 2
                entries.take(limit).forEach { entry ->
                    ChecklistRow(entry, entry.id in done, { onCheck(entry, it) }, date = today, pending = CompletionGate.key(entry.id, today) in pending)
                }
                if (entries.size > limit) TextButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) { Text(if (id == JourneyCard.PRAYERS) "View full prayer checklist" else "View all ${entries.size} entries") }
            }
            if (size == JourneySize.COMPACT && id != JourneyCard.PRAYERS) TextButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) { Text("Open $title") }
        }
    }
}

@Composable
private fun JourneyLight14(summary: ProgressSummary, prayersDone: Int, today: LocalDate, prefs: NurPreferences, size: String, collapsed: Boolean, onFold: () -> Unit) {
    val target = NurMotion.fraction(summary.fraction)
    val progress = rememberNurProgress(target, today, "Daily Light")
    val scheme = MaterialTheme.colorScheme
    val reduce = LocalNurReduceMotion.current
    NurPanel(Modifier.fillMaxWidth().then(if (reduce) Modifier else Modifier.animateContentSize())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("DAILY LIGHT", style = MaterialTheme.typography.labelSmall, color = scheme.primary, fontWeight = FontWeight.SemiBold)
                Text("A little light, every day", style = MaterialTheme.typography.titleLarge)
            }
            IconButton(onClick = onFold, modifier = Modifier.size(48.dp)) { Icon(if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess, contentDescription = if (collapsed) "Expand Daily Light" else "Fold Daily Light") }
        }
        if (!collapsed) {
            if (size != JourneySize.COMPACT) {
                val diameter = if (size == JourneySize.EXPANDED) 206.dp else 166.dp
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(diameter).clearAndSetSemantics {
                        contentDescription = "Daily Light: ${summary.completed} of ${summary.total} completed. $prayersDone of 5 prayers."
                        progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f)
                    }, contentAlignment = Alignment.Center) {
                        NurCircularProgress(prayersDone / 5f, today, "Prayer ring", modifier = Modifier.fillMaxSize(), strokeWidth = 3.dp, color = scheme.primary.copy(alpha = 0.7f))
                        NurCircularProgress(target, today, "Daily Light ring", modifier = Modifier.fillMaxSize(0.80f), strokeWidth = 10.dp)
                        listOf(Alignment.TopCenter, Alignment.BottomCenter, Alignment.CenterStart, Alignment.CenterEnd).forEach { alignment ->
                            Box(Modifier.fillMaxSize(), contentAlignment = alignment) { Icon(Icons.Default.Star, null, tint = scheme.primary, modifier = Modifier.size(11.dp)) }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الله", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = scheme.primary)
                            Text("${NurMotion.percent(progress)}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Today's progress", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                Text("${summary.completed} / ${summary.total}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            NurLinearProgress(target, today, "Daily Light total progress")
            Text("$prayersDone of 5 prayers completed", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            if (size == JourneySize.EXPANDED && prefs.showArabic) {
                HorizontalDivider(color = scheme.outlineVariant)
                Text("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleSmall, color = scheme.primary)
            }
        }
    }
}
