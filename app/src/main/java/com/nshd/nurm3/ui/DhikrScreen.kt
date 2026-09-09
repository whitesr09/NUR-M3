package com.nshd.nurm3.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Personal counting aid. Recorded counts, not animation or optimistic state, are authoritative. */
@Composable
fun DhikrScreen(model: NurViewModel, prefs: NurPreferences, today: LocalDate) {
    val snapshots by model.dhikrSnapshots.collectAsStateWithLifecycle()
    val allPhrases by model.dhikrPhrases.collectAsStateWithLifecycle()
    val days by model.dhikrDays.collectAsStateWithLifecycle()
    val pending by model.dhikrPending.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val snackbar = LocalNurSnackbarHost.current
    var selectedId by rememberSaveable { mutableStateOf("") }
    var editorId by rememberSaveable { mutableStateOf<String?>(null) }
    var resetId by rememberSaveable { mutableStateOf<String?>(null) }
    var archiveId by rememberSaveable { mutableStateOf<String?>(null) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var historyFilter by rememberSaveable { mutableStateOf("all") }
    var busy by remember { mutableStateOf(false) }
    val selected = snapshots.firstOrNull { it.phrase.id == selectedId } ?: snapshots.firstOrNull()
    val archived = allPhrases.filter { it.archived }
    val pendingTotal = pending.values.sum()
    val canManage = !busy && pendingTotal == 0

    fun notice(text: String) {
        scope.launch { snackbar?.showSnackbar(text) }
    }

    fun runTask(block: suspend () -> Unit) {
        if (busy || pending.values.any { it > 0 }) return
        busy = true
        scope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                snackbar?.showSnackbar("Could not save your change. Your existing records are unchanged.")
            } finally {
                busy = false
            }
        }
    }

    LazyColumn(contentPadding = PaddingValues(NurDesign.pagePadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            NurPageHeading("Your remembrance", "Dhikr", "A calm, personal counter with your recorded history.")
        }
        if (snapshots.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                    items(snapshots, key = { it.phrase.id }) { snapshot ->
                        NurChoicePill(snapshot.phrase.title, selected?.phrase?.id == snapshot.phrase.id) {
                            selectedId = snapshot.phrase.id
                        }
                    }
                }
            }
            selected?.let { snapshot ->
                val phrase = snapshot.phrase
                val waiting = pending[phrase.id] ?: 0
                item(key = "counter-${phrase.id}") {
                    NurPanel(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(phrase.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                            Text("PERSONAL SESSION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text("Goal ${phrase.target}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            DhikrCounterRing(phrase, today)
                            if (phrase.sessionCount >= phrase.target) {
                                Text("Personal goal reached · You can continue counting", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                            }
                        }
                        NurPrimaryAction(
                            text = "Tap to count", onClick = {
                                scope.launch {
                                    try {
                                        if (model.incrementDhikr(phrase.id)) {
                                            if (prefs.dhikrHaptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } else notice("Counter limit reached or this phrase is no longer active.")
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        notice("This count could not be saved. Please try again.")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp),
                            enabled = !busy && phrase.sessionCount < DhikrRules.MAX_COUNT,
                            icon = { Icon(Icons.Default.TouchApp, contentDescription = null) }
                        )
                        if (waiting > 0) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text("Saving $waiting ${if (waiting == 1) "tap" else "taps"}…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { editorId = phrase.id }, enabled = canManage, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Edit")
                            }
                            OutlinedButton(onClick = { resetId = phrase.id }, enabled = canManage && phrase.sessionCount > 0, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                                Text("Reset session")
                            }
                        }
                        TextButton(onClick = { archiveId = phrase.id }, enabled = canManage, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Archive phrase")
                        }
                    }
                }
                item(key = "totals-${phrase.id}") {
                    NurPanel(Modifier.fillMaxWidth()) {
                        Text("Your recorded counts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DhikrMetric("Today", snapshot.todayCount.toString(), Modifier.weight(1f))
                            DhikrMetric("Lifetime", snapshot.lifetimeCount.toString(), Modifier.weight(1f))
                        }
                        Text("Session counts continue across midnight until you reset them. Daily totals use the local date when each count was recorded.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item(key = "history-${phrase.id}") {
                    val rows = remember(days, phrase.id) { DhikrHistory.rows(days, phrase.id) }
                    DhikrHistoryPanel(
                        phrase.title, rows, today, showHistory, historyFilter,
                        onToggle = { showHistory = !showHistory }, onFilter = { historyFilter = it }
                    )
                }
            }
        } else {
            item {
                NurEmptyState(Icons.Default.TouchApp, "No active phrases", "Add a personal phrase or restore one from the archive. Existing history is preserved.", "Add a phrase") { editorId = "" }
            }
        }
        item {
            NurPrimaryAction("Add a custom phrase", { editorId = "" }, Modifier.fillMaxWidth(), enabled = canManage, icon = { Icon(Icons.Default.Add, contentDescription = null) })
        }
        item {
            NurSettingRow("Gentle tap feedback", "Optional haptic feedback after a count is saved", prefs.dhikrHaptics) {
                model.setting("dhikr_haptics", it)
            }
        }
        if (archived.isNotEmpty()) {
            item {
                TextButton(onClick = { showArchived = !showArchived }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (showArchived) "Hide archived phrases" else "Manage archived phrases (${archived.size})")
                    Spacer(Modifier.width(6.dp))
                    Icon(if (showArchived) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                }
            }
            if (showArchived) items(archived, key = { "archived-${it.id}" }) { phrase ->
                NurPanel(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(phrase.title, style = MaterialTheme.typography.titleSmall)
                            Text("Session ${phrase.sessionCount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { runTask { model.restoreDhikr(phrase.id); selectedId = phrase.id } }, enabled = canManage) { Text("Restore") }
                    }
                }
            }
        }
        item {
            Text("Targets are personal counting aids, not religious rulings or promises of reward. Consult a reliable source for prescribed forms and counts of remembrance.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }
    }

    editorId?.let { id ->
        val existing = allPhrases.firstOrNull { it.id == id }
        if (id.isEmpty() || (existing != null && !existing.archived)) {
            DhikrEditor(existing = existing, saving = busy, onDismiss = { if (!busy) editorId = null }, onSave = { title, target ->
                runTask {
                    if (id.isEmpty()) model.addDhikr(title, target) else model.updateDhikr(id, title, target)
                    editorId = null
                }
            })
        } else {
            LaunchedEffect(id) { editorId = null }
        }
    }
    resetId?.let { id ->
        val phrase = allPhrases.firstOrNull { it.id == id }
        if (phrase == null || phrase.archived) {
            LaunchedEffect(id) { resetId = null }
        } else {
            AlertDialog(
                onDismissRequest = { if (!busy) resetId = null },
                title = { Text("Reset this session?") },
                text = { Text("The session for ${phrase.title} will return to zero. Today's and lifetime recorded counts will remain unchanged.") },
                confirmButton = { TextButton(onClick = { resetId = null; runTask { model.resetDhikrSession(id) } }, enabled = canManage) { Text("Reset session") } },
                dismissButton = { TextButton(onClick = { resetId = null }) { Text("Cancel") } }
            )
        }
    }
    archiveId?.let { id ->
        val phrase = allPhrases.firstOrNull { it.id == id }
        if (phrase == null || phrase.archived) {
            LaunchedEffect(id) { archiveId = null }
        } else {
            AlertDialog(
                onDismissRequest = { if (!busy) archiveId = null },
                title = { Text("Archive phrase?") },
                text = { Text("${phrase.title} will leave the active counter list. Its session and dated history are preserved, and you can restore it later.") },
                confirmButton = { TextButton(onClick = { archiveId = null; runTask { model.archiveDhikr(id); selectedId = "" } }, enabled = canManage) { Text("Archive") } },
                dismissButton = { TextButton(onClick = { archiveId = null }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
private fun DhikrCounterRing(phrase: DhikrPhrase, today: LocalDate) {
    val progress = DhikrRules.progress(phrase.sessionCount, phrase.target)
    val animated = rememberNurProgress(progress, today, "dhikr-${phrase.id}")
    val scheme = MaterialTheme.colorScheme
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val diameter = minOf(maxWidth, 240.dp)
        Box(Modifier.size(diameter).clearAndSetSemantics {
            contentDescription = "Session ${phrase.sessionCount} of personal goal ${phrase.target}"
            progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
        }, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxSize().padding(7.dp), strokeWidth = 3.dp, color = scheme.primary.copy(alpha = 0.42f), trackColor = scheme.outlineVariant)
            CircularProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxSize().padding(19.dp), strokeWidth = 9.dp, color = scheme.primary, trackColor = scheme.surfaceVariant)
            Column(Modifier.fillMaxWidth().padding(horizontal = 34.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(phrase.sessionCount.toString(), style = if (phrase.sessionCount > 999_999_999) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2, softWrap = true)
                Text("SESSION", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DhikrMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, softWrap = true)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DhikrHistoryPanel(
    phraseTitle: String,
    rows: List<DhikrHistoryRow>,
    today: LocalDate,
    expanded: Boolean,
    filter: String,
    onToggle: () -> Unit,
    onFilter: (String) -> Unit
) {
    val visible = remember(rows, today, filter) { if (filter == "recent") DhikrHistory.recent(rows, today) else rows }
    val formatter = remember { DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault()) }
    NurPanel(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Recorded history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${rows.size} recorded ${if (rows.size == 1) "day" else "days"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onToggle, modifier = Modifier.size(48.dp)) {
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = if (expanded) "Hide $phraseTitle history" else "Show $phraseTitle history")
            }
        }
        if (expanded) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NurChoicePill("All recorded", filter == "all") { onFilter("all") }
                NurChoicePill("Last 30 days", filter == "recent") { onFilter("recent") }
            }
            if (visible.isEmpty()) {
                Text("No recorded counts in this period. No dates have been added or estimated.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                visible.forEach { row ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(row.date.format(formatter), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(row.count.toString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
            Text("These are saved counts by device-local date. Resetting a session does not erase them.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DhikrEditor(existing: DhikrPhrase?, saving: Boolean, onDismiss: () -> Unit, onSave: (String, Int) -> Unit) {
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var target by rememberSaveable(existing?.id) { mutableStateOf((existing?.target ?: 33).toString()) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val parsed = target.toIntOrNull()
    val valid = title.isNotBlank() && title.length <= 200 && parsed != null && parsed in 1..DhikrRules.MAX_TARGET
    val dirty = title != (existing?.title ?: "") || target != (existing?.target ?: 33).toString()
    fun requestClose() {
        if (saving) return
        if (dirty) confirmDiscard = true else onDismiss()
    }
    ModalBottomSheet(onDismissRequest = ::requestClose) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (existing == null) "New Dhikr phrase" else "Edit phrase", style = MaterialTheme.typography.headlineSmall)
            Text("Choose a personal label and goal. Your recorded history stays separate from the session.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(value = title, onValueChange = { if (it.length <= 200) title = it }, label = { Text("Phrase or personal label") }, enabled = !saving, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, supportingText = { Text("${title.length}/200 characters") })
            OutlinedTextField(value = target, onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) target = it }, label = { Text("Personal goal") }, enabled = !saving, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, isError = target.isNotEmpty() && parsed !in 1..DhikrRules.MAX_TARGET, supportingText = { Text("Choose 1–100,000. This is not a prescribed religious count.") })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(33, 99, 100).forEach { value -> NurChoicePill(value.toString(), parsed == value && !saving) { if (!saving) target = value.toString() } }
            }
            if (saving) LinearProgressIndicator(Modifier.fillMaxWidth())
            NurPrimaryAction(if (saving) "Saving…" else "Save phrase", { if (valid && !saving) onSave(title.trim(), parsed!!) }, Modifier.fillMaxWidth(), enabled = valid && !saving)
            TextButton(onClick = ::requestClose, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Cancel") }
            Spacer(Modifier.height(16.dp))
        }
    }
    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false },
        title = { Text("Discard your changes?") },
        text = { Text("Your unsaved edits will be lost. Existing counts and history will not change.") },
        confirmButton = { TextButton(onClick = { confirmDiscard = false; onDismiss() }) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } }
    )
}
