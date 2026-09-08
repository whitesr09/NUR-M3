package com.nshd.nurm3.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.data.*
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Import never starts until the complete file has passed validation. */
@Composable
fun BackupScreen(model: NurViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingRaw by remember { mutableStateOf<String?>(null) }
    var summary by remember { mutableStateOf<BackupSummary?>(null) }
    var preparedExport by remember { mutableStateOf<String?>(null) }
    var confirmReplace by remember { mutableStateOf(false) }

    fun perform(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        message = null
        scope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { message = e.message ?: "Operation failed. Your existing data was not intentionally changed." }
            finally { busy = false }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val json = preparedExport
        preparedExport = null
        if (uri != null && json != null) perform {
            BackupFiles.write(context, uri, json)
            message = "Backup saved successfully. Keep this file somewhere safe."
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) perform {
            val raw = BackupFiles.read(context, uri)
            val checked = model.inspectBackup(raw)
            pendingRaw = raw
            summary = checked
            message = null
        }
    }
    val safetyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val raw = pendingRaw
        if (uri != null && raw != null) perform {
            // A separately saved copy of existing data is required before replacement.
            val safetyCopy = model.exportBackup()
            BackupFiles.write(context, uri, safetyCopy)
            val result = model.importBackup(raw, replace = true)
            message = "Restore complete: ${result.entriesAdded} entries and ${result.completionsAdded} completion records. Your safety copy was saved."
            pendingRaw = null
            summary = null
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Your data, in your hands", style = MaterialTheme.typography.headlineMedium)
        Text("Export your saved entries and recorded history. Import a backup to recover or move your data. All processing happens on your device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Export backup", style = MaterialTheme.typography.titleLarge)
                Text("Includes prayers, tasks, reflections, habits, recurring schedules, archived entries, and actual completion history. It does not include passwords or API keys.")
                Button(onClick = { perform { preparedExport = model.exportBackup(); exportLauncher.launch("NUR-M3-${LocalDate.now()}.json") } }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Choose where to save") }
            }
        }
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Import backup", style = MaterialTheme.typography.titleLarge)
                Text("Select a NUR-M3 JSON backup. The file is checked before you can restore it.")
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Choose backup file") }
            }
        }
        val checked = summary
        val raw = pendingRaw
        if (checked != null && raw != null) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Backup preview", style = MaterialTheme.typography.titleLarge)
                    Text("${checked.entryCount} entries • ${checked.completionCount} completion records")
                    Text("Schema ${checked.schema}. Existing data will remain unchanged until you choose an import method.", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { perform {
                        val result = model.importBackup(raw, replace = false)
                        message = "Merge complete: ${result.entriesAdded} new entries and ${result.completionsAdded} new completion records. Existing records were preserved."
                        pendingRaw = null; summary = null
                    } }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Merge without deleting") }
                    OutlinedButton(onClick = { confirmReplace = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Replace with backup…") }
                    Text("Merge skips existing IDs and dates. Replace requires you to save a safety copy first. Neither operation can recover data from an old app that has already been uninstalled without an export.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text("A backup is a local file, not automatic cloud synchronization. Keep a copy outside your phone before uninstalling or changing signing certificates.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (confirmReplace) AlertDialog(
        onDismissRequest = { confirmReplace = false },
        title = { Text("Replace all current data?") },
        text = { Text("Current entries and completion history will be replaced with the selected backup. You must first save a safety copy to a location you choose. If you cancel the save, no replacement occurs.") },
        confirmButton = { TextButton(onClick = { confirmReplace = false; safetyLauncher.launch("NUR-M3-before-restore-${LocalDate.now()}.json") }) { Text("Save safety copy") } },
        dismissButton = { TextButton(onClick = { confirmReplace = false }) { Text("Cancel") } }
    )
}
