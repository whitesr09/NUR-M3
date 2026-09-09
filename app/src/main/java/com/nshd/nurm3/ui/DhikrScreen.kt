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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.data.*
import java.time.LocalDate
import kotlinx.coroutines.launch

/** Counting is an optional personal aid. No target is presented as a religious prescription. */
@Composable
fun DhikrScreen(model: NurViewModel, prefs: NurPreferences, today: LocalDate) {
    val snapshots by model.dhikrSnapshots.collectAsStateWithLifecycle()
    val allPhrases by model.dhikrPhrases.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var selectedId by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<DhikrPhrase?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var reset by remember { mutableStateOf<DhikrPhrase?>(null) }
    var archive by remember { mutableStateOf<DhikrPhrase?>(null) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val selected = snapshots.firstOrNull { it.phrase.id == selectedId } ?: snapshots.firstOrNull()
    val archived = allPhrases.filter { it.archived }

    fun runTask(block: suspend () -> Unit) {
        if (busy) return
        scope.launch {
            busy = true
            message = ""
            try { block() } catch (error: Exception) { message = error.message ?: "Could not save your change." }
            finally { busy = false }
        }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Dhikr", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text("A quiet space for remembrance. Your counts stay on this device and are included in NUR backups.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (snapshots.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(snapshots, key = { it.phrase.id }) { snapshot ->
                        FilterChip(selected = selected?.phrase?.id == snapshot.phrase.id, onClick = { selectedId = snapshot.phrase.id }, label = { Text(snapshot.phrase.title) })
                    }
                }
            }
            selected?.let { snapshot ->
                item(key = snapshot.phrase.id) {
                    val phrase = snapshot.phrase
                    val progress = DhikrRules.progress(phrase.sessionCount, phrase.target)
                    val animated = rememberNurProgress(progress, today, "dhikr-${phrase.id}")
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(phrase.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text("Personal goal: ${phrase.target}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Box(Modifier.size(210.dp).clearAndSetSemantics {
                                contentDescription = "Session ${phrase.sessionCount} of personal goal ${phrase.target}"
                                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                            }, contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(progress = { animated }, modifier = Modifier.size(196.dp), strokeWidth = 9.dp, color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(phrase.sessionCount.toString(), style = MaterialTheme.typography.headlineLarge.copy(fontSize = if (phrase.sessionCount > 999_999) 26.sp else 40.sp), fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text("Session", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (phrase.sessionCount >= phrase.target) Text("Personal goal reached", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                            Button(onClick = {
                                scope.launch {
                                    try {
                                        if (model.incrementDhikr(phrase.id)) {
                                            message = ""
                                            if (prefs.dhikrHaptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } else message = "Counter limit reached or this phrase is no longer active."
                                    } catch (error: Exception) { message = error.message ?: "Count could not be saved." }
                                }
                            }, enabled = !busy && phrase.sessionCount < DhikrRules.MAX_COUNT, modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp)) {
                                Icon(Icons.Default.TouchApp, contentDescription = null)
                                Spacer(Modifier.width(12.dp))
                                Text("Tap to count", style = MaterialTheme.typography.titleMedium)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { editing = phrase }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Edit") }
                                OutlinedButton(onClick = { reset = phrase }, enabled = !busy && phrase.sessionCount > 0, modifier = Modifier.weight(1f)) { Text("Reset session") }
                            }
                            TextButton(onClick = { archive = phrase }, enabled = !busy) { Text("Archive phrase") }
                        }
                    }
                }
                item {
                    SectionCard("Your recorded counts", "A session reset never deletes dated history", null) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { Text("Today", style = MaterialTheme.typography.labelMedium); Text(snapshot.todayCount.toString(), style = MaterialTheme.typography.titleLarge) }
                            Column(horizontalAlignment = Alignment.End) { Text("Lifetime", style = MaterialTheme.typography.labelMedium); Text(snapshot.lifetimeCount.toString(), style = MaterialTheme.typography.titleLarge) }
                        }
                        Text("Session counts continue across midnight until you reset them. Daily totals start on the new local date, while earlier records remain saved.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            item { Text("No active phrases. Add one, or restore an archived phrase below.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        item {
            OutlinedButton(onClick = { adding = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add a custom phrase")
            }
        }
        item { SettingToggle("Gentle tap feedback", "Optional haptic feedback after a count is saved", prefs.dhikrHaptics) { model.setting("dhikr_haptics", it) } }
        if (archived.isNotEmpty()) {
            item { TextButton(onClick = { showArchived = !showArchived }) { Text(if (showArchived) "Hide archived phrases" else "Manage archived phrases (${archived.size})") } }
            if (showArchived) items(archived, key = { it.id }) { phrase ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(phrase.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        TextButton(onClick = { runTask { model.restoreDhikr(phrase.id); selectedId = phrase.id } }, enabled = !busy) { Text("Restore") }
                    }
                }
            }
        }
        if (busy) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        if (message.isNotBlank()) item { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Text("Targets are personal counting aids, not religious rulings or promises of reward. Consult a reliable source for prescribed forms and counts of remembrance.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }

    if (adding || editing != null) DhikrEditor(editing, onDismiss = { adding = false; editing = null }, onSave = { title, target ->
        val existing = editing
        runTask {
            if (existing == null) model.addDhikr(title, target) else model.updateDhikr(existing.id, title, target)
            adding = false
            editing = null
        }
    })
    reset?.let { phrase ->
        AlertDialog(onDismissRequest = { reset = null }, title = { Text("Reset this session?") }, text = { Text("The session for ${phrase.title} will return to zero. Today's and lifetime recorded counts will remain unchanged.") }, confirmButton = { TextButton(onClick = { reset = null; runTask { model.resetDhikrSession(phrase.id) } }) { Text("Reset session") } }, dismissButton = { TextButton(onClick = { reset = null }) { Text("Cancel") } })
    }
    archive?.let { phrase ->
        AlertDialog(onDismissRequest = { archive = null }, title = { Text("Archive phrase?") }, text = { Text("${phrase.title} will leave the active counter list. Its session and dated history are preserved, and you can restore it later.") }, confirmButton = { TextButton(onClick = { archive = null; runTask { model.archiveDhikr(phrase.id); selectedId = "" } }) { Text("Archive") } }, dismissButton = { TextButton(onClick = { archive = null }) { Text("Cancel") } })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DhikrEditor(existing: DhikrPhrase?, onDismiss: () -> Unit, onSave: (String, Int) -> Unit) {
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var target by rememberSaveable(existing?.id) { mutableStateOf((existing?.target ?: 33).toString()) }
    val parsed = target.toIntOrNull()
    val valid = title.isNotBlank() && title.length <= 200 && parsed != null && parsed in 1..DhikrRules.MAX_TARGET
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (existing == null) "New Dhikr phrase" else "Edit phrase", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(value = title, onValueChange = { if (it.length <= 200) title = it }, label = { Text("Phrase or personal label") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = target, onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) target = it }, label = { Text("Personal target") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth(), supportingText = { Text("Choose 1–100,000. This is not a prescribed religious count.") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(33, 99, 100).forEach { value -> FilterChip(selected = parsed == value, onClick = { target = value.toString() }, label = { Text(value.toString()) }) }
            }
            Button(onClick = { if (valid) onSave(title.trim(), parsed!!) }, enabled = valid, modifier = Modifier.fillMaxWidth()) { Text("Save phrase") }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            Spacer(Modifier.height(16.dp))
        }
    }
}
