package com.nshd.nurm3.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.data.*
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Optional quick-create overlay used only by the widget destination. */
@Composable
fun EntryScreen(title: String, subtitle: String, kind: String, entries: List<Entry>, completions: List<Completion>, today: LocalDate, model: NurViewModel, initialAdd: Boolean) {
    EntryScreen(title, subtitle, kind, entries, completions, today, model)
    var show by rememberSaveable(initialAdd) { mutableStateOf(initialAdd) }
    var text by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    if (show) AlertDialog(
        onDismissRequest = { if (!saving) show = false },
        title = { Text("Quick add Amanah") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add a task that remains saved after midnight. Only its daily checkbox resets.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(text, { text = it.take(200) }, label = { Text("Task title") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(enabled = !saving && text.isNotBlank(), onClick = {
            saving = true
            scope.launch {
                try {
                    val entry = Entry(UUID.randomUUID().toString(), kind, text.trim(), 0, System.currentTimeMillis(), startDate = LocalDate.now().toString())
                    if (model.persistEntry(entry, true)) { show = false; text = "" }
                    else error = "Please enter a valid task."
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { error = "Could not save the task. Try again." }
                finally { saving = false }
            }
        }) { Text(if (saving) "Saving…" else "Save task") } },
        dismissButton = { TextButton(enabled = !saving, onClick = { show = false }) { Text("Cancel") } }
    )
}
