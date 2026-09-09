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
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.data.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(title: String, subtitle: String, kind: String, entries: List<Entry>, completions: List<Completion>, today: LocalDate, model: NurViewModel) {
    val section = entries.filter { it.kind == kind }
    val active = section.filter { EntrySchedule.isActive(it, today) }
    val done = DailyProgress.completedIds(completions, today)
    val summary = DailyProgress.summary(active, completions, today)
    var editing by remember { mutableStateOf<Entry?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Entry?>(null) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add ${if (kind == NurKind.RHYTHM) "habit" else "entry"}")
            }
        }
        item {
            SectionCard("Today's progress", "${summary.completed} of ${summary.total} completed", null) {
                NurLinearProgress(summary.fraction, today, "$title progress")
            }
        }
        if (section.isEmpty()) item { Text("No entries yet. Add one above to begin.", style = MaterialTheme.typography.bodyMedium) }
        if (active.isNotEmpty()) item { Text("Scheduled today", style = MaterialTheme.typography.titleMedium) }
        items(active, key = { it.id }) { entry ->
            EntryCard(entry, entry.id in done, { model.complete(entry.id, today, it) }, { editing = entry }, { deleting = entry }, date = today)
        }
        val other = section.filterNot { it in active }
        if (other.isNotEmpty()) {
            item { Text("Other saved entries", style = MaterialTheme.typography.titleMedium); Text("These are not scheduled today, but remain available to edit.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(other, key = { it.id }) { entry ->
                EntryCard(entry, false, {}, { editing = entry }, { deleting = entry }, enabled = false, date = today)
            }
        }
    }
    if (adding || editing != null) EntryEditor(kind, editing, today, onDismiss = { adding = false; editing = null }, onSave = { entry ->
        if (editing == null) model.addEntry(entry) else model.updateEntry(entry)
        adding = false; editing = null
    })
    deleting?.let { entry ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Archive entry?") }, text = { Text("${entry.title} will leave your active list. Its recorded history will be kept.") }, confirmButton = { TextButton(onClick = { model.delete(entry.id); deleting = null }) { Text("Archive") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } })
    }
}

@Composable
private fun EntryCard(entry: Entry, checked: Boolean, onChecked: (Boolean) -> Unit, onEdit: () -> Unit, onArchive: () -> Unit, enabled: Boolean = true, date: LocalDate) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        ChecklistRow(entry, checked, onChecked, enabled = enabled, trailing = {
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit ${entry.title}") }
            IconButton(onClick = onArchive) { Icon(Icons.Default.DeleteOutline, contentDescription = "Archive ${entry.title}") }
        }, date = date)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryEditor(kind: String, existing: Entry?, today: LocalDate, onDismiss: () -> Unit, onSave: (Entry) -> Unit) {
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var schedule by rememberSaveable(existing?.id) { mutableStateOf(existing?.schedule ?: "daily") }
    var mask by rememberSaveable(existing?.id) { mutableIntStateOf(existing?.weekdaysMask ?: 127) }
    var start by rememberSaveable(existing?.id) { mutableStateOf(existing?.startDate ?: today.toString()) }
    var end by rememberSaveable(existing?.id) { mutableStateOf(existing?.endDate ?: "") }
    var dateTarget by remember { mutableStateOf<String?>(null) }
    val startDate = LocalDate.parse(start)
    val endValid = end.isBlank() || !LocalDate.parse(end).isBefore(startDate)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (existing == null) "New entry" else "Edit entry", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(value = title, onValueChange = { if (it.length <= 200) title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Text("Repeat", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("daily" to "Daily", "weekdays" to "Days", "weekly" to "Weekly", "once" to "Once").forEach { option ->
                    FilterChip(selected = schedule == option.first, onClick = { schedule = option.first }, label = { Text(option.second) })
                }
            }
            if (schedule == "weekdays") {
                Text("Repeat on", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { index, label ->
                        val bit = 1 shl index
                        FilterChip(selected = mask and bit != 0, onClick = { mask = mask xor bit }, label = { Text(label) }, modifier = Modifier.weight(1f))
                    }
                }
            }
            OutlinedButton(onClick = { dateTarget = "start" }, modifier = Modifier.fillMaxWidth()) { Text("Starts: $start") }
            if (schedule != "once") OutlinedButton(onClick = { dateTarget = "end" }, modifier = Modifier.fillMaxWidth()) { Text(if (end.isBlank()) "Optional end date" else "Ends: $end") }
            if (end.isNotBlank() && schedule != "once") TextButton(onClick = { end = "" }) { Text("Clear end date") }
            if (!endValid) Text("End date must be on or after the start date.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Button(onClick = {
                val entry = existing?.copy(title = title.trim(), schedule = schedule, weekdaysMask = mask, startDate = start, endDate = if (schedule == "once") start else end.ifBlank { null })
                    ?: Entry(UUID.randomUUID().toString(), kind, title.trim(), 0, System.currentTimeMillis(), schedule = schedule, weekdaysMask = mask, startDate = start, endDate = if (schedule == "once") start else end.ifBlank { null })
                onSave(entry)
            }, enabled = title.isNotBlank() && endValid && (schedule != "weekdays" || mask != 0), modifier = Modifier.fillMaxWidth()) { Text("Save entry") }
            Spacer(Modifier.height(16.dp))
        }
    }
    if (dateTarget != null) {
        val target = dateTarget!!
        val initial = if (target == "start") start else end.ifBlank { today.toString() }
        val picker = rememberDatePickerState(initialSelectedDateMillis = LocalDate.parse(initial).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { dateTarget = null }, confirmButton = {
            TextButton(onClick = {
                picker.selectedDateMillis?.let { millis ->
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    if (target == "start") start = date else end = date
                }
                dateTarget = null
            }) { Text("OK") }
        }, dismissButton = { TextButton(onClick = { dateTarget = null }) { Text("Cancel") } }) { DatePicker(state = picker) }
    }
}

@Composable
fun HistoryScreen(entries: List<Entry>, completions: List<Completion>) {
    val dates = completions.map { it.localDate }.distinct().sortedDescending()
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("History", style = MaterialTheme.typography.headlineMedium); Text("Your actual recorded activity, never invented or backfilled.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (dates.isEmpty()) item { Text("No completed days recorded yet.") }
        items(dates, key = { it }) { date ->
            val records = completions.filter { it.localDate == date }
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(date, style = MaterialTheme.typography.titleMedium)
                    Text("${records.size} recorded completions", color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider()
                    records.forEach { record ->
                        val entry = entries.firstOrNull { it.id == record.entryId }
                        val name = record.titleSnapshot.ifBlank { entry?.title ?: "Archived entry" }
                        Text("• $name", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
