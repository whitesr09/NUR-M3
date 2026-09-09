package com.nshd.nurm3.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nshd.nurm3.focus.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusScreen(controller: FocusController = viewModel()) {
    val active by controller.active.collectAsStateWithLifecycle()
    val remaining by controller.remaining.collectAsStateWithLifecycle()
    val sessions by controller.sessions.collectAsStateWithLifecycle()
    val routines by controller.routines.collectAsStateWithLifecycle()
    val message by controller.message.collectAsStateWithLifecycle()
    var label by rememberSaveable { mutableStateOf("Focused work") }
    var minutes by rememberSaveable { mutableIntStateOf(25) }
    var rest by rememberSaveable { mutableIntStateOf(5) }
    var confirmCancel by remember { mutableStateOf(false) }
    val running = active?.status == "running"
    val completed = sessions.filter(FocusRules::completed)
    val today = java.time.LocalDate.now().toString()
    val todayCount = completed.count { it.finishedAt?.let { time -> Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate().toString() == today } == true }
    LazyColumn(contentPadding = PaddingValues(NurDesign.pagePadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { NurPageHeading("Work with intention", "Focus & Routine", "A quiet space for real work, rest and consistent habits.") }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary)
                    Text(if (running) "Focus in progress" else if (active?.status == "paused") "Ready to continue" else "Focus timer", style = MaterialTheme.typography.titleMedium)
                }
                val target = active?.plannedSeconds ?: minutes * 60
                val left = if (active != null) remaining else target
                Text("%02d:%02d".format(left / 60, left % 60), modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.primary)
                NurLinearProgress(if (target == 0) 0f else 1f - left.toFloat() / target, java.time.LocalDate.MIN, "Focus progress")
                Text(active?.label ?: label, style = MaterialTheme.typography.titleMedium)
                Text("${todayCount} completed today · ${completed.size} total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (active?.status == "paused") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = controller::resume, modifier = Modifier.weight(1f)) { Icon(Icons.Default.PlayArrow, null); Text("Resume") }
                        OutlinedButton(onClick = { confirmCancel = true }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    }
                } else if (running) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = controller::pause, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Pause, null); Text("Pause") }
                        OutlinedButton(onClick = { confirmCancel = true }, modifier = Modifier.weight(1f)) { Text("Stop") }
                    }
                } else {
                    OutlinedTextField(label, { if (it.length <= 120) label = it }, label = { Text("What are you working on?") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Text("Work interval: $minutes minutes", style = MaterialTheme.typography.titleSmall)
                    Slider(value = minutes.toFloat(), onValueChange = { minutes = it.toInt() }, valueRange = 1f..120f, steps = 118)
                    NurPrimaryAction("Start focus", { controller.start(label, minutes) }, modifier = Modifier.fillMaxWidth(), enabled = label.isNotBlank(), icon = { Icon(Icons.Default.PlayArrow, null) })
                }
                Text("Only active foreground time is counted. Leaving NUR pauses the timer; a saved interrupted session can be resumed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Text("Routine Studio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Build reusable work and rest presets. Routines never mark your tasks or prayers complete.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(label, { if (it.length <= 100) label = it }, label = { Text("Routine name") }, modifier = Modifier.fillMaxWidth())
                Text("Rest interval: $rest minutes")
                Slider(value = rest.toFloat(), onValueChange = { rest = it.toInt() }, valueRange = 1f..60f, steps = 58)
                OutlinedButton(onClick = { controller.saveRoutine(label, minutes, rest) }, enabled = label.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Text("Save routine") }
                routines.forEach { routine ->
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(routine.name, style = MaterialTheme.typography.titleSmall)
                            Text("${routine.workMinutes} min focus · ${routine.restMinutes} min rest", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { label = routine.name; minutes = routine.workMinutes; rest = routine.restMinutes }) { Text("Use") }
                        IconButton(onClick = { controller.deleteRoutine(routine.id) }) { Icon(Icons.Default.DeleteOutline, "Delete ${routine.name}") }
                    }
                }
            }
        }
        item { Text("Session history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        if (sessions.isEmpty()) item { NurEmptyState(Icons.Default.History, "No focus sessions yet", "Finish a session to begin your genuine focus history.") }
        items(sessions.size) { index ->
            val session = sessions[index]
            NurPanel(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(session.label, style = MaterialTheme.typography.titleSmall)
                        Text("${session.elapsedSeconds / 60} of ${session.plannedSeconds / 60} min · ${session.status.replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Instant.ofEpochMilli(session.startedAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a")), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (session.status == "paused" || session.status == "running" && !running) TextButton(onClick = { controller.restore(session) }) { Text("Resume") }
                }
            }
        }
        item { Text("A completed session is recorded only after its full interval. Opening a timer does not create a completion.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    if (confirmCancel) AlertDialog(onDismissRequest = { confirmCancel = false }, title = { Text("Stop this session?") }, text = { Text("The unfinished interval will remain in history as cancelled, not completed.") }, confirmButton = { TextButton(onClick = { controller.cancel(); confirmCancel = false }) { Text("Stop session") } }, dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Keep focusing") } })
}
