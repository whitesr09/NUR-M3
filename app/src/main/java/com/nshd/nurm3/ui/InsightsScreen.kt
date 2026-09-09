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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val reviewRanges = listOf(7 to "7 days", 30 to "30 days", 90 to "90 days")
private val reviewKinds = listOf(
    "all" to "All", NurKind.PRAYER to "Prayers", NurKind.AMANAH to "Amanah",
    NurKind.MUHASABA to "Muhasaba", NurKind.RHYTHM to "Rhythm"
)

/** Read-only review. All values come from stored entries and completion records. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsightsScreen(entries: List<Entry>, completions: List<Completion>, today: LocalDate) {
    var range by rememberSaveable { mutableIntStateOf(7) }
    var kind by rememberSaveable { mutableStateOf("all") }
    var query by rememberSaveable { mutableStateOf("") }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var visibleCount by rememberSaveable(range, kind) { mutableIntStateOf(14) }
    val result = remember(entries, completions, today, range, kind) {
        ReviewInsights.review(entries, completions, today, range, kind)
    }
    val summary = result.summary
    val days = result.days.asReversed()
    val habits = entries.filter { it.kind == NurKind.RHYTHM && (showArchived || !it.archived) && it.title.contains(query.trim(), ignoreCase = true) }
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEE, d MMM") }

    LazyColumn(contentPadding = PaddingValues(NurDesign.pagePadding), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            NurPageHeading("Your activity", "Insights", "A clearer view of your real progress, without invented check-ins or scores.")
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Review period", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    reviewRanges.forEach { (value, label) ->
                        NurChoicePill(label, range == value, { range = value })
                    }
                }
                Text("Activity type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    reviewKinds.forEach { (value, label) ->
                        NurChoicePill(label, kind == value, { kind = value })
                    }
                }
            }
        }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(118.dp), contentAlignment = Alignment.Center) {
                        NurCircularProgress(summary.fraction, today, "Scheduled completion rate", Modifier.fillMaxSize(), strokeWidth = 9.dp)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (summary.scheduled == 0) "—" else "${NurMotion.percent(summary.fraction)}%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                            Text("SCHEDULED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Scheduled progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${summary.completed} of ${summary.scheduled} scheduled actions", style = MaterialTheme.typography.bodyMedium)
                        Text(if (summary.days == 0) "No saved activity is available for this period." else "${summary.days} days available in this review", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                NurLinearProgress(summary.fraction, today, "Review scheduled progress")
                Text("The denominator uses currently active saved schedules. Archived entries are not treated as active, but their actual check-ins remain in recorded activity and History.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReviewMetric("Recorded", summary.recorded.toString(), "Saved check-ins", Modifier.weight(1f))
                ReviewMetric("Active days", summary.recordedDays.toString(), "Days with records", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            ReviewMetric("Fully completed days", summary.perfectDays.toString(), "Days when every currently scheduled action was checked", Modifier.fillMaxWidth())
        }
        item {
            Text("Daily activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("Only dates from your known saved activity are shown. Future records are excluded.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (days.isEmpty()) item {
            NurEmptyState(Icons.Default.Insights, "No activity to review", "Your first saved entries and check-ins will appear here. Nothing has been added to your history automatically.")
        }
        items(days.take(visibleCount), key = { it.date.toString() }) { day ->
            ReviewDayCard(day, today, dateFormat)
        }
        if (visibleCount < days.size) item {
            OutlinedButton(onClick = { visibleCount = (visibleCount + 14).coerceAtMost(days.size) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Show more days · ${days.size - visibleCount} remaining")
            }
        }
        if (kind == "all" || kind == NurKind.RHYTHM) {
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("Rhythm streaks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("See consistency for your saved habits. An unfinished current day does not break yesterday's streak.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search habits") }, leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = if (query.isNotBlank()) ({ IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Clear search") } }) else null, shape = MaterialTheme.shapes.medium)
                Spacer(Modifier.height(8.dp))
                NurSettingRow("Include archived habits", "Show historical records from habits you have archived", showArchived) { showArchived = it }
            }
            if (habits.isEmpty()) item {
                NurEmptyState(Icons.Default.Repeat, "No matching habits", "Add a Rhythm habit or change your search to review saved consistency.")
            }
            items(habits, key = { it.id }) { entry ->
                val insight = remember(entry, completions, today) { NurInsights.habit(if (entry.archived) entry.copy(archived = false) else entry, completions, today) }
                NurPanel(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(if (entry.archived) "Archived · historical review" else "Active habit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.Repeat, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ReviewMetric("Recorded days", insight.completedDays.toString(), "Saved check-ins", Modifier.weight(1f))
                        ReviewMetric("Longest", insight.longestStreak.toString(), "Scheduled days", Modifier.weight(1f))
                    }
                    if (!entry.archived) Text("Current streak: ${insight.currentStreak} scheduled days", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    else Text("Historical streaks use the saved recurrence definition. The archive date is not recorded, so no current streak is claimed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Text("Insights are a private reflection tool, not a measure of faith or personal worth. A completion is counted only when a real record exists. All-time records and exact timestamps remain available in History.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ReviewMetric(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReviewDayCard(day: ReviewDay, today: LocalDate, formatter: DateTimeFormatter) {
    NurPanel(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(day.date.format(formatter), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(if (day.scheduled.total == 0) "No active schedule" else "${day.scheduled.completed} of ${day.scheduled.total} scheduled", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${day.recordedCount} recorded", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        NurLinearProgress(day.scheduled.fraction, today, "Progress for ${day.date}")
    }
}
