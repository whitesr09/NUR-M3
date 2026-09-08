package com.nshd.nurm3.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun InsightsScreen(entries: List<Entry>, completions: List<Completion>, today: LocalDate, reduceMotion: Boolean) {
    var period by remember { mutableIntStateOf(7) }
    val report = remember(completions, today, period) { ActivityInsights.report(completions, today, period) }
    val names = mapOf(NurKind.PRAYER to "Prayers", NurKind.AMANAH to "Amanah", NurKind.MUHASABA to "Muhasaba", NurKind.RHYTHM to "Rhythm")
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Your activity", style = MaterialTheme.typography.headlineMedium)
            Text("A clear view of what you actually recorded, without invented history or pressure to be perfect.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(7, 28, 90).forEachIndexed { index, value ->
                    SegmentedButton(selected = period == value, onClick = { period = value }, shape = SegmentedButtonDefaults.itemShape(index, 3)) { Text("${value}d") }
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("${report.recordedCompletions}", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                    Text("Recorded completions in $period days", style = MaterialTheme.typography.titleMedium)
                    Text("${report.recordedDays} days with recorded activity", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider()
                    names.forEach { (kind, label) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label)
                            Text("${report.byKind[kind] ?: 0}", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        item {
            Text("Daily activity", style = MaterialTheme.typography.titleLarge)
            Text("Bars compare recorded actions, not a percentage of historical obligations.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val maximum = report.days.maxOfOrNull { it.total }?.coerceAtLeast(1) ?: 1
        items(report.days.asReversed(), key = { it.date.toString() }) { day ->
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(day.date.format(DateTimeFormatter.ofPattern("EEE, d MMM")), style = MaterialTheme.typography.bodyMedium)
                    Text("${day.total}", style = MaterialTheme.typography.labelLarge)
                }
                NurLinearProgress(day.total.toFloat() / maximum, "${day.total} recorded actions", reduceMotion)
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text("Habit streaks", style = MaterialTheme.typography.titleLarge)
            Text("Based on each habit's current recurring schedule. Changing a schedule may change the estimate for older days.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val habits = entries.filter { it.kind == NurKind.RHYTHM && !it.archived }
        if (habits.isEmpty()) item { Text("Add a Rhythm habit to begin tracking consistency.") }
        items(habits, key = { it.id }) { entry ->
            val streak = remember(entry, completions, today) { ActivityInsights.currentStreak(entry, completions, today) }
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(entry.title, style = MaterialTheme.typography.titleMedium)
                        Text("Current schedule", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("$streak", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { Text("NUR records completion events only. A missing record does not prove that an action was not performed. Historical percentages are not reconstructed from edited or archived task definitions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
