package com.nshd.nurm3.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.data.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryScreen(title: String, subtitle: String, kind: String, entries: List<Entry>, completions: List<Completion>, today: LocalDate, model: NurViewModel, initialAdd: Boolean = false) {
    val section = entries.filter { it.kind == kind }
    val active = section.filter { EntrySchedule.isActive(it, today) }
    val done = DailyProgress.completedIds(completions, today)
    val summary = DailyProgress.summary(active, completions, today)
    val pending by model.completionPending.collectAsStateWithLifecycle()
    val allEntries by model.allEntries.collectAsStateWithLifecycle()
    val archived = allEntries.filter { it.kind == kind && it.archived }
    val scope = rememberCoroutineScope()
    val snackbar = LocalNurSnackbarHost.current
    var editing by remember { mutableStateOf<Entry?>(null) }
    var adding by rememberSaveable { mutableStateOf(initialAdd) }
    var deleting by remember { mutableStateOf<Entry?>(null) }
    var archivingId by remember { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    val matches: (Entry) -> Boolean = { it.title.contains(query.trim(), ignoreCase = true) }
    val visible = active.filter { matches(it) && when (filter) {
        "open" -> it.id !in done
        "done" -> it.id in done
        else -> true
    } }
    val other = section.filter { !EntrySchedule.isActive(it, today) && matches(it) }
    val itemName = if (kind == NurKind.RHYTHM) "habit" else "entry"

    LazyColumn(contentPadding = NurScrollContentPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            NurPageHeading("Your daily practice", title, subtitle)
            Spacer(Modifier.height(14.dp))
            NurPrimaryAction("Add $itemName", { adding = true }, Modifier.fillMaxWidth(), icon = { Icon(Icons.Default.Add, null, Modifier.size(20.dp)) })
        }
        item {
            SectionCard("Today's progress", "${summary.completed} of ${summary.total} completed", null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (summary.total == 0) "No items scheduled" else "Keep your momentum", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${NurMotion.percent(summary.fraction)}%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
                NurLinearProgress(summary.fraction, today, "$title progress")
            }
        }
        if (section.isNotEmpty()) {
            item {
                OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search $title") }, leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = if (query.isNotBlank()) ({ IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Clear search") } }) else null, shape = MaterialTheme.shapes.medium)
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("all" to "All", "open" to "To do", "done" to "Completed").forEach { option ->
                        NurChoicePill(option.second, filter == option.first, { filter = option.first })
                    }
                }
            }
            if (active.isNotEmpty()) item { Text("Scheduled today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        }
        if (section.isEmpty()) item {
            NurEmptyState(Icons.Default.Checklist, "Your space is ready", "Add your first $itemName to begin. Your entries will remain saved until you archive them.", "Add $itemName") { adding = true }
        } else if (visible.isEmpty()) item {
            NurEmptyState(Icons.Default.SearchOff, if (filter == "done") "Nothing completed yet" else "No matching entries", "Try another search or change the filter. Your saved entries have not been removed.")
        }
        items(visible, key = { it.id }) { entry ->
            val busy = CompletionGate.key(entry.id, today) in pending || archivingId == entry.id
            EntryCard(entry, entry.id in done, { model.complete(entry.id, today, it) }, { editing = entry }, { deleting = entry }, enabled = true, busy = busy, date = today)
        }
        if (filter == "all" && other.isNotEmpty()) {
            item {
                Text("Other saved entries", style = MaterialTheme.typography.titleMedium)
                Text("Not scheduled today. You can still edit or archive them.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(other, key = { it.id }) { entry ->
                EntryCard(entry, false, {}, { editing = entry }, { deleting = entry }, enabled = false, busy = archivingId == entry.id, date = today)
            }
        }
        if (archived.isNotEmpty()) {
            item { TextButton(onClick = { showArchived = !showArchived }) { Text(if (showArchived) "Hide archived entries" else "Archived entries (${archived.size})") } }
            if (showArchived) items(archived, key = { it.id }) { entry ->
                NurPanel(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.title, style = MaterialTheme.typography.bodyLarge)
                            Text("History preserved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { scope.launch {
                            try { if (!model.restoreEntry(entry.id)) snackbar?.showSnackbar("Could not restore this entry.") }
                            catch (error: Exception) { if (error is CancellationException) throw error; snackbar?.showSnackbar("Restore failed. Try again.") }
                        } }) { Text("Restore") }
                    }
                }
            }
        }
    }

    if (adding || editing != null) EntryEditor(kind, editing, today,
        onDismiss = { adding = false; editing = null },
        onSave = { entry -> model.persistEntry(entry, editing == null) },
        onSaved = { adding = false; editing = null }
    )
    deleting?.let { entry ->
        AlertDialog(onDismissRequest = { if (archivingId == null) deleting = null },
            icon = { Icon(Icons.Default.Archive, null) },
            title = { Text("Archive entry?") },
            text = { Text("${entry.title} will leave your active list. Its recorded history will be kept, and you can restore it later.") },
            confirmButton = { TextButton(enabled = archivingId == null, onClick = {
                if (archivingId != null) return@TextButton
                archivingId = entry.id
                scope.launch {
                    try {
                        if (model.archiveEntry(entry.id)) {
                            deleting = null
                            archivingId = null
                            if (snackbar != null) {
                                val result = snackbar.showSnackbar("Entry archived", actionLabel = "Undo", withDismissAction = true, duration = SnackbarDuration.Short)
                                if (result == SnackbarResult.ActionPerformed) model.restoreEntry(entry.id)
                            }
                        } else snackbar?.showSnackbar("This entry could not be archived.")
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        snackbar?.showSnackbar("Archive failed. Your entry is still available.")
                    } finally { archivingId = null }
                }
            }) { Text(if (archivingId == null) "Archive" else "Saving…") } },
            dismissButton = { TextButton(enabled = archivingId == null, onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun EntryCard(entry: Entry, checked: Boolean, onChecked: (Boolean) -> Unit, onEdit: () -> Unit, onArchive: () -> Unit, enabled: Boolean, busy: Boolean, date: LocalDate) {
    var menu by remember { mutableStateOf(false) }
    ChecklistRow(entry, checked, onChecked, enabled = enabled, pending = busy, date = date, trailing = {
        Box {
            IconButton(onClick = { menu = true }, enabled = !busy, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.MoreHoriz, contentDescription = "More options for ${entry.title}")
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Edit") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { menu = false; onEdit() })
                DropdownMenuItem(text = { Text("Archive") }, leadingIcon = { Icon(Icons.Default.Archive, null) }, onClick = { menu = false; onArchive() })
            }
        }
    })
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EntryEditor(kind: String, existing: Entry?, today: LocalDate, onDismiss: () -> Unit, onSave: suspend (Entry) -> Boolean, onSaved: () -> Unit) {
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var schedule by rememberSaveable(existing?.id) { mutableStateOf(existing?.schedule ?: "daily") }
    var mask by rememberSaveable(existing?.id) { mutableIntStateOf(existing?.weekdaysMask ?: 127) }
    var start by rememberSaveable(existing?.id) { mutableStateOf(existing?.startDate ?: today.toString()) }
    var end by rememberSaveable(existing?.id) { mutableStateOf(existing?.endDate ?: "") }
    var dateTarget by remember { mutableStateOf<String?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val startDate = runCatching { LocalDate.parse(start) }.getOrNull()
    val endDate = if (end.isBlank()) null else runCatching { LocalDate.parse(end) }.getOrNull()
    val endValid = schedule == "once" || end.isBlank() || (startDate != null && endDate != null && !endDate.isBefore(startDate))
    val valid = title.isNotBlank() && title.length <= 200 && startDate != null && endValid && (schedule != "weekdays" || mask != 0)
    val dirty = title != (existing?.title ?: "") || schedule != (existing?.schedule ?: "daily") || mask != (existing?.weekdaysMask ?: 127) || start != (existing?.startDate ?: today.toString()) || end != (existing?.endDate ?: "")
    fun requestDismiss() { if (!saving) { if (dirty) confirmDiscard = true else onDismiss() } }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = ::requestDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (existing == null) "New entry" else "Edit entry", modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = ::requestDismiss, enabled = !saving) { Icon(Icons.Default.Close, "Close editor") }
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    OutlinedTextField(value = title, onValueChange = { if (it.length <= 200) title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), maxLines = 3, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), supportingText = { Text("${title.length}/200 characters") })
                }
                item {
                    Text("Repeat", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("daily" to "Daily", "weekdays" to "Days", "weekly" to "Weekly", "once" to "Once").forEach { option ->
                            NurChoicePill(option.second, schedule == option.first, { schedule = option.first })
                        }
                    }
                }
                if (schedule == "weekdays") item {
                    Text("Repeat on", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEachIndexed { index, label ->
                            val bit = 1 shl index
                            NurChoicePill(label, mask and bit != 0, { mask = mask xor bit })
                        }
                    }
                    if (mask == 0) Text("Choose at least one day.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                item {
                    Text("Schedule dates", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { dateTarget = "start" }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = MaterialTheme.shapes.medium) {
                        Icon(Icons.Default.CalendarMonth, null); Spacer(Modifier.width(8.dp)); Text("Starts: $start")
                    }
                    if (schedule != "once") {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { dateTarget = "end" }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = MaterialTheme.shapes.medium) {
                            Icon(Icons.Default.Event, null); Spacer(Modifier.width(8.dp)); Text(if (end.isBlank()) "Optional end date" else "Ends: $end")
                        }
                        if (end.isNotBlank()) TextButton(onClick = { end = "" }) { Text("Clear end date") }
                    }
                    if (!endValid) Text("End date must be on or after the start date.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                item { Text("Your saved entry stays available across midnight. Only its daily completion state changes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (saveError.isNotBlank()) Text(saveError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = ::requestDismiss, enabled = !saving, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("Cancel") }
                    NurPrimaryAction(if (saving) "Saving…" else "Save entry", onClick = {
                        if (!valid || saving) return@NurPrimaryAction
                        val entry = existing?.copy(title = title.trim(), schedule = schedule, weekdaysMask = mask, startDate = start, endDate = if (schedule == "once") start else end.ifBlank { null })
                            ?: Entry(UUID.randomUUID().toString(), kind, title.trim(), 0, System.currentTimeMillis(), schedule = schedule, weekdaysMask = mask, startDate = start, endDate = if (schedule == "once") start else end.ifBlank { null })
                        scope.launch {
                            saving = true
                            saveError = ""
                            try { if (onSave(entry)) onSaved() else saveError = "Please check the entry details and try again." }
                            catch (error: Exception) { if (error is CancellationException) throw error; saveError = "Could not save this entry. Your changes are still here." }
                            finally { saving = false }
                        }
                    }, modifier = Modifier.weight(1.5f), enabled = valid && !saving, icon = if (saving) ({ CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary) }) else null)
                }
            }
        }
    }
    if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false }, title = { Text("Discard changes?") }, text = { Text("Your unsaved changes will be lost. Your existing saved entry will not be changed.") }, confirmButton = { TextButton(onClick = { confirmDiscard = false; onDismiss() }) { Text("Discard") } }, dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } })
    if (dateTarget != null) {
        val target = dateTarget!!
        val initial = runCatching { LocalDate.parse(if (target == "start") start else end.ifBlank { today.toString() }) }.getOrDefault(today)
        val picker = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { dateTarget = null }, confirmButton = {
            TextButton(onClick = {
                picker.selectedDateMillis?.let { millis ->
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    if (target == "start") start = date else end = date
                }
                dateTarget = null
            }, enabled = picker.selectedDateMillis != null) { Text("OK") }
        }, dismissButton = { TextButton(onClick = { dateTarget = null }) { Text("Cancel") } }) { DatePicker(state = picker) }
    }
}

@Composable
fun HistoryScreen(entries: List<Entry>, completions: List<Completion>) {
    val dates = completions.map { it.localDate }.distinct().sortedDescending()
    val byId = remember(entries) { entries.associateBy { it.id } }
    var expanded by rememberSaveable { mutableStateOf<String?>(dates.firstOrNull()) }
    LazyColumn(contentPadding = NurScrollContentPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { NurPageHeading("Your record", "History", "Only your actual saved completions appear here.") }
        if (dates.isEmpty()) item { NurEmptyState(Icons.Default.History, "Your history starts here", "Complete a prayer, task or habit to see your first recorded day.") }
        items(dates, key = { it }) { date ->
            val records = completions.filter { it.localDate == date }
            val label = runCatching { LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy")) }.getOrDefault(date)
            SectionCard(label, "${records.size} recorded completions", { expanded = if (expanded == date) null else date }) {
                if (expanded == date) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    records.forEach { record ->
                        val entry = byId[record.entryId]
                        val name = record.titleSnapshot.ifBlank { entry?.title ?: "Archived entry" }
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                val kind = record.kindSnapshot.ifBlank { entry?.kind ?: "Entry" }
                                val time = runCatching { Instant.ofEpochMilli(record.completedAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("h:mm a")) }.getOrDefault("")
                                Text("$kind${if (time.isBlank()) "" else " • $time"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else Text("Tap the arrow to view this day's records.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
