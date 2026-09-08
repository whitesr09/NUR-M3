package com.nshd.nurm3.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun InsightsScreen(entries: List<Entry>, completions: List<Completion>, today: LocalDate) {
    val days = NurInsights.daily(entries, completions, today.minusDays(6), today)
    val completed = days.sumOf { it.summary.completed }
    val scheduled = days.sumOf { it.summary.total }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Insights", style = MaterialTheme.typography.headlineMedium)
            Text("Your actual activity over the last seven days.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { SectionCard("This week", "$completed of $scheduled scheduled actions", null) {
            LinearProgressIndicator(progress = { if (scheduled == 0) 0f else completed.toFloat() / scheduled }, modifier = Modifier.fillMaxWidth())
            Text("Progress is calculated from your saved completion records.", style = MaterialTheme.typography.bodySmall)
        } }
        item { Text("Daily activity", style = MaterialTheme.typography.titleLarge) }
        items(days.reversed(), key = { it.date.toString() }) { day ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(day.date.format(DateTimeFormatter.ofPattern("EEE, d MMM")))
                        Text("${day.summary.completed}/${day.summary.total}", color = MaterialTheme.colorScheme.primary)
                    }
                    LinearProgressIndicator(progress = { day.summary.fraction }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item { Text("Rhythm streaks", style = MaterialTheme.typography.titleLarge) }
        val habits = entries.filter { it.kind == NurKind.RHYTHM && !it.archived }
        if (habits.isEmpty()) item { Text("Add a Rhythm habit to see your streaks.") }
        items(habits, key = { it.id }) { entry ->
            val insight = NurInsights.habit(entry, completions, today)
            SectionCard(entry.title, "${insight.completedDays} recorded days", null) {
                Text("Current streak: ${insight.currentStreak} scheduled days")
                Text("Longest streak: ${insight.longestStreak} scheduled days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { Text("An unfinished current day does not break yesterday’s streak. Future or unscheduled completions do not count.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
