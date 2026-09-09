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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.data.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val dueFormat = DateTimeFormatter.ofPattern("d MMM, h:mm a")
private fun dueLabel(value: Long?): String = value?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(dueFormat) } ?: "No due date"
private fun priorityLabel(value: Int) = when (value) { 1 -> "High"; 3 -> "Low"; else -> "Normal" }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AmanahPowerScreen(model: NurViewModel, navigate: (String) -> Unit) {
    val context = LocalContext.current
    val repo = remember(context) { AmanahRepository(NurExtrasDatabase.get(context), NurDatabase.get(context)) }
    val entries by model.entries.collectAsStateWithLifecycle()
    val completions by model.completions.collectAsStateWithLifecycle()
    val today by model.today.collectAsStateWithLifecycle()
    val details by repo.details.collectAsStateWithLifecycle(initialValue = emptyList())
    val templates by repo.templates.collectAsStateWithLifecycle(initialValue = emptyList())
    val pending by model.completionPending.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = LocalNurSnackbarHost.current
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var editor by remember { mutableStateOf<Entry?>(null) }
    var creating by remember { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf("today") }
    var query by rememberSaveable { mutableStateOf("") }
    var showTemplates by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val tasks = entries.filter { it.kind == NurKind.AMANAH }
    val metadata = details.associateBy { it.entryId }
    val done = DailyProgress.completedIds(completions, today)
    val now = System.currentTimeMillis()
    val visible = tasks.filter { entry ->
        val due = metadata[entry.id]?.dueAt
        val checked = entry.id in done
        entry.title.contains(query, true) && when (filter) {
            "today" -> EntrySchedule.isActive(entry, today)
            "upcoming" -> !checked && (due != null && due > now || !EntrySchedule.isActive(entry, today))
            "overdue" -> !checked && due != null && due < now
            "completed" -> checked
            else -> true
        }
    }.sortedWith(compareBy<Entry> { metadata[it.id]?.priority ?: 2 }.thenBy { metadata[it.id]?.dueAt ?: Long.MAX_VALUE }.thenBy { it.position })

    if (selected != null) {
        val entry = entries.firstOrNull { it.id == selected }
        if (entry != null) {
            AmanahDetailScreen(entry, repo, model, today, entry.id in done, metadata[entry.id], { selected = null }, { editor = entry }, navigate)
        } else LaunchedEffect(selected) { selected = null }
    } else {
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                NurPageHeading("Your responsibilities", "Amanah", "Plan meaningful work and keep your saved history.")
                Spacer(Modifier.height(12.dp))
                NurPrimaryAction("New task", { creating = true }, Modifier.fillMaxWidth(), icon = { Icon(Icons.Default.Add, null) })
            }
            item {
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Search tasks") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("today" to "Today", "upcoming" to "Upcoming", "overdue" to "Overdue", "completed" to "Completed", "all" to "All").forEach { (id, label) ->
                        NurChoicePill(label, filter == id, { filter = id })
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${visible.size} tasks", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { showTemplates = !showTemplates }) { Text("Templates") }
                }
            }
            if (showTemplates) {
                if (templates.isEmpty()) item { Text("Save a task as a template to reuse its details and subtasks.", style = MaterialTheme.typography.bodySmall) }
                items(templates, key = { "template-${it.id}" }) { template ->
                    NurPanel(Modifier.fillMaxWidth()) {
                        Text(template.title, style = MaterialTheme.typography.titleSmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(enabled = !busy, onClick = {
                                busy = true
                                scope.launch {
                                    try { repo.applyTemplate(template.id, today); snackbar?.showSnackbar("Task created from template.") }
                                    catch (e: Exception) { if (e is CancellationException) throw e; snackbar?.showSnackbar("Could not apply template.") }
                                    finally { busy = false }
                                }
                            }) { Text("Use template") }
                        }
                    }
                }
            }
            if (visible.isEmpty()) item { NurEmptyState(Icons.Default.Checklist, "Nothing here yet", "Try another filter or create a new task.", "New task") { creating = true } }
            items(visible, key = { it.id }) { entry ->
                val detail = metadata[entry.id]
                val saving = CompletionGate.key(entry.id, today) in pending
                NurPanel(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(priorityLabel(detail?.priority ?: 2), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.weight(1f))
                        Text(detail?.category.orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    ChecklistRow(entry, entry.id in done, { model.complete(entry.id, today, it) }, enabled = EntrySchedule.isActive(entry, today) && today == LocalDate.now(), date = today, pending = saving, trailing = {
                        IconButton(onClick = { selected = entry.id }, enabled = !saving) { Icon(Icons.Default.ChevronRight, "Open task details") }
                    })
                    if (detail != null) Text(dueLabel(detail.dueAt), style = MaterialTheme.typography.bodySmall, color = if (detail.dueAt != null && detail.dueAt < now && entry.id !in done) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { TextButton(onClick = { navigate("amanah-basic") }) { Text("Classic checklist & recurrence settings") } }
        }
    }
    if (creating || editor != null) AmanahPowerEditor(editor, metadata[editor?.id], repo, model, today, { creating = false; editor = null }) { id ->
        creating = false; editor = null; selected = id
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmanahPowerEditor(existing: Entry?, details: TaskDetails?, repo: AmanahRepository, model: NurViewModel, today: LocalDate, onDismiss: () -> Unit, onSaved: (String) -> Unit) {
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var priority by rememberSaveable(existing?.id) { mutableIntStateOf(details?.priority ?: 2) }
    var category by rememberSaveable(existing?.id) { mutableStateOf(details?.category.orEmpty()) }
    var notes by rememberSaveable(existing?.id) { mutableStateOf(details?.notes.orEmpty()) }
    val initialDue = details?.dueAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
    var date by rememberSaveable(existing?.id) { mutableStateOf(initialDue?.toLocalDate()?.toString().orEmpty()) }
    var time by rememberSaveable(existing?.id) { mutableStateOf(initialDue?.toLocalTime()?.format(DateTimeFormatter.ofPattern("HH:mm")).orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var discard by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val due = if (date.isBlank() && time.isBlank()) null else runCatching {
        LocalDate.parse(date).atTime(LocalTime.parse(time)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrNull()
    val valid = title.isNotBlank() && title.length <= 200 && category.length <= 80 && notes.length <= 10_000 && ((date.isBlank() && time.isBlank()) || due != null)
    val dirty = title != existing?.title.orEmpty() || priority != (details?.priority ?: 2) || category != details?.category.orEmpty() || notes != details?.notes.orEmpty() || date != initialDue?.toLocalDate()?.toString().orEmpty() || time != initialDue?.toLocalTime()?.format(DateTimeFormatter.ofPattern("HH:mm")).orEmpty()
    fun close() { if (!saving) { if (dirty) discard = true else onDismiss() } }
    ModalBottomSheet(onDismissRequest = ::close, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).imePadding()) {
            Text(if (existing == null) "New task" else "Edit task", Modifier.padding(20.dp), style = MaterialTheme.typography.headlineSmall)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item { OutlinedTextField(title, { if (it.length <= 200) title = it }, Modifier.fillMaxWidth(), label = { Text("Task title") }) }
                item {
                    Text("Priority", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(1 to "High", 2 to "Normal", 3 to "Low").forEach { (id, label) -> NurChoicePill(label, priority == id, { priority = id }) } }
                }
                item { OutlinedTextField(category, { if (it.length <= 80) category = it }, Modifier.fillMaxWidth(), label = { Text("Category") }) }
                item { OutlinedTextField(notes, { if (it.length <= 10_000) notes = it }, Modifier.fillMaxWidth(), label = { Text("Notes") }, minLines = 3) }
                item {
                    Text("Optional due date & time", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth(), label = { Text("Date (YYYY-MM-DD)") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(time, { time = it }, Modifier.fillMaxWidth(), label = { Text("Time (HH:mm)") }, singleLine = true)
                    if (date.isNotBlank() || time.isNotBlank()) TextButton(onClick = { date = ""; time = "" }) { Text("Clear due date") }
                }
                item { Text("Task definitions remain saved after midnight. Use the classic editor to change recurrence or start/end dates.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            HorizontalDivider()
            Column(Modifier.padding(20.dp)) {
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = ::close, enabled = !saving, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    NurPrimaryAction(if (saving) "Saving…" else "Save", onClick = {
                        if (!valid || saving) return@NurPrimaryAction
                        saving = true; error = ""
                        scope.launch {
                            try {
                                val detail = TaskDetails(existing?.id.orEmpty(), priority, category.trim(), notes, due)
                                val id = if (existing == null) repo.createTask(title, today, detail) else {
                                    if (!model.persistEntry(existing.copy(title = title.trim()), false)) error("Could not update task")
                                    if (!repo.saveDetails(detail)) error("Could not save details")
                                    existing.id
                                }
                                onSaved(id)
                            } catch (e: Exception) { if (e is CancellationException) throw e; error = e.message ?: "Could not save task." }
                            finally { saving = false }
                        }
                    }, modifier = Modifier.weight(1f), enabled = valid && !saving)
                }
            }
        }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard changes?") }, text = { Text("Unsaved edits will be lost. Your saved task remains unchanged.") }, confirmButton = { TextButton(onClick = { discard = false; onDismiss() }) { Text("Discard") } }, dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep editing") } })
}

@Composable
private fun AmanahDetailScreen(entry: Entry, repo: AmanahRepository, model: NurViewModel, today: LocalDate, completed: Boolean, details: TaskDetails?, onBack: () -> Unit, onEdit: () -> Unit, navigate: (String) -> Unit) {
    val children by remember(repo, entry.id) { repo.subtasks(entry.id) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val checks by remember(repo, today) { repo.checks(today) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val pending by model.completionPending.collectAsStateWithLifecycle()
    val done = checks.map { it.subtaskId }.toSet()
    val scope = rememberCoroutineScope()
    val snackbar = LocalNurSnackbarHost.current
    var newChild by rememberSaveable(entry.id) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var archive by remember { mutableStateOf(false) }
    var templateName by remember { mutableStateOf<String?>(null) }
    var childToArchive by remember { mutableStateOf<Subtask?>(null) }
    var editChild by remember { mutableStateOf<Subtask?>(null) }
    var editChildText by remember { mutableStateOf("") }
    val saving = CompletionGate.key(entry.id, today) in pending
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TextButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null); Text("Back to tasks") } }
        item {
            NurPageHeading("Amanah", entry.title, "${priorityLabel(details?.priority ?: 2)} priority${details?.category?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}")
            Spacer(Modifier.height(12.dp))
            ChecklistRow(entry, completed, { model.complete(entry.id, today, it) }, enabled = EntrySchedule.isActive(entry, today) && today == LocalDate.now(), date = today, pending = saving)
        }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Text("Task details", style = MaterialTheme.typography.titleMedium)
                Text(dueLabel(details?.dueAt), style = MaterialTheme.typography.bodyMedium)
                if (!details?.notes.isNullOrBlank()) Text(details!!.notes, style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("Edit") }
                    OutlinedButton(onClick = { navigate("reminders?entryId=${entry.id}") }, modifier = Modifier.weight(1f)) { Text("Reminder") }
                }
            }
        }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Text("Subtasks · ${children.count { it.id in done }} of ${children.size}", style = MaterialTheme.typography.titleMedium)
                NurLinearProgress(if (children.isEmpty()) 0f else children.count { it.id in done }.toFloat() / children.size, today, "Subtask progress")
                Text("Subtasks do not add extra Daily Light requirements. Their checkboxes refresh by date, not by deleting saved definitions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                children.forEach { child ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = child.id in done, onCheckedChange = { checked ->
                            if (busy) return@Checkbox
                            busy = true
                            scope.launch {
                                try { if (!repo.checkSubtask(child.id, today, checked)) snackbar?.showSnackbar("Subtask is no longer scheduled today.") }
                                catch (e: Exception) { if (e is CancellationException) throw e; snackbar?.showSnackbar("Could not save subtask.") }
                                finally { busy = false }
                            }
                        }, enabled = !busy && EntrySchedule.isActive(entry, today) && today == LocalDate.now())
                        Text(child.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { editChild = child; editChildText = child.title }) { Icon(Icons.Default.Edit, "Edit subtask") }
                        IconButton(onClick = { childToArchive = child }) { Icon(Icons.Default.Archive, "Archive subtask") }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(newChild, { if (it.length <= 200) newChild = it }, Modifier.weight(1f), label = { Text("Add a subtask") }, singleLine = true)
                    IconButton(enabled = newChild.isNotBlank() && !busy, onClick = {
                        busy = true
                        scope.launch {
                            try { if (repo.addSubtask(entry.id, newChild)) newChild = "" else snackbar?.showSnackbar("Cannot add subtask.") }
                            catch (e: Exception) { if (e is CancellationException) throw e; snackbar?.showSnackbar(e.message ?: "Could not add subtask.") }
                            finally { busy = false }
                        }
                    }) { Icon(Icons.Default.AddCircle, "Save subtask") }
                }
            }
        }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Text("More options", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { templateName = entry.title }) { Text("Save as reusable template") }
                TextButton(onClick = { archive = true }) { Text("Archive task") }
                Text("Archiving preserves dated completions and task details.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (archive) AlertDialog(onDismissRequest = { archive = false }, title = { Text("Archive task?") }, text = { Text("The task leaves your active list, but its recorded history remains.") }, confirmButton = { TextButton(enabled = !busy, onClick = {
        busy = true
        scope.launch {
            try {
                if (model.archiveEntry(entry.id)) {
                    archive = false; onBack()
                    val result = snackbar?.showSnackbar("Task archived", actionLabel = "Undo", duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) model.restoreEntry(entry.id)
                }
            } catch (e: Exception) { if (e is CancellationException) throw e; snackbar?.showSnackbar("Archive failed.") }
            finally { busy = false }
        }
    }) { Text("Archive") } }, dismissButton = { TextButton(onClick = { archive = false }) { Text("Cancel") } })
    if (templateName != null) AlertDialog(onDismissRequest = { templateName = null }, title = { Text("Save template") }, text = { OutlinedTextField(templateName.orEmpty(), { templateName = it.take(200) }, label = { Text("Template name") }) }, confirmButton = { TextButton(enabled = !busy && !templateName.isNullOrBlank(), onClick = {
        val name = templateName.orEmpty(); busy = true
        scope.launch {
            try { if (repo.saveTemplate(entry.id, name)) { templateName = null; snackbar?.showSnackbar("Template saved.") } }
            catch (e: Exception) { if (e is CancellationException) throw e; snackbar?.showSnackbar("Could not save template.") }
            finally { busy = false }
        }
    }) { Text("Save") } }, dismissButton = { TextButton(onClick = { templateName = null }) { Text("Cancel") } })
    childToArchive?.let { child -> AlertDialog(onDismissRequest = { childToArchive = null }, title = { Text("Archive subtask?") }, text = { Text("Its recorded completions will be preserved.") }, confirmButton = { TextButton(onClick = { scope.launch { repo.archiveSubtask(child.id); childToArchive = null } }) { Text("Archive") } }, dismissButton = { TextButton(onClick = { childToArchive = null }) { Text("Cancel") } }) }
    editChild?.let { child -> AlertDialog(onDismissRequest = { editChild = null }, title = { Text("Edit subtask") }, text = { OutlinedTextField(editChildText, { editChildText = it.take(200) }, label = { Text("Title") }) }, confirmButton = { TextButton(enabled = editChildText.isNotBlank(), onClick = { scope.launch { if (repo.updateSubtask(child.id, editChildText)) editChild = null } }) { Text("Save") } }, dismissButton = { TextButton(onClick = { editChild = null }) { Text("Cancel") } }) }
}
