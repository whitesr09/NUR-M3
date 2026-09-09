package com.nshd.nurm3.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.data.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
fun BackupScreen() {
    val context = LocalContext.current
    val manager = remember(context) { BackupManager(context, NurDatabase.get(context)) }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<BackupPayload?>(null) }
    var source by remember { mutableStateOf("") }
    var confirmReplace by remember { mutableStateOf(false) }
    var recoveryAvailable by remember { mutableStateOf(false) }
    fun runTask(block: suspend () -> Unit) {
        if (!busy) scope.launch {
            busy = true
            try { block() } catch (error: Exception) { message = error.message ?: "The operation could not be completed." }
            finally { busy = false }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) runTask {
            val data = manager.export()
            manager.write(uri, data)
            message = "Backup exported successfully. Keep this file somewhere safe."
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) runTask {
            val payload = BackupCodec.decode(manager.read(uri))
            pending = payload
            source = "Selected backup"
            message = ""
        }
    }
    LaunchedEffect(Unit) { recoveryAvailable = manager.recovery() != null }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Backup & restore", style = MaterialTheme.typography.headlineMedium)
            Text("Your data stays yours. Export before reinstalling or changing signing certificates.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { SectionCard("Export your data", "Entries, recurrence, completion history and Dhikr counts", null) {
            Text("The JSON file is readable and unencrypted. It includes your personal records but not API keys, PINs or lock credentials.", style = MaterialTheme.typography.bodySmall)
            Button(onClick = { exportLauncher.launch("NUR-M3-${LocalDate.now()}.json") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save backup file")
            }
        } }
        item { SectionCard("Import a backup", "Review the contents before changing anything", null) {
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.FileOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Choose backup file")
            }
        } }
        if (recoveryAvailable) item { SectionCard("Local recovery", "A previous replace operation preserved a recovery snapshot", null) {
            OutlinedButton(onClick = { runTask { pending = manager.recovery(); source = "Local recovery" } }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Review recovery snapshot") }
        } }
        if (busy) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()); Text("Processing locally…", style = MaterialTheme.typography.bodySmall) }
        if (message.isNotBlank()) item { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        pending?.let { payload ->
            item { SectionCard("Review $source", "No data has been changed yet", null) {
                val date = runCatching { Instant.ofEpochMilli(payload.createdAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")) }.getOrDefault("Unknown")
                Text("Created: $date")
                Text("${payload.entries.size} saved entries")
                Text("${payload.completions.size} completion records")
                if (payload.schema >= 3) {
                    Text("${payload.dhikrPhrases.size} Dhikr phrases")
                    Text("${payload.dhikrDays.size} dated Dhikr records")
                } else Text("Older backup (version ${payload.schema}): no Dhikr data. Your current Dhikr counters will be kept during replacement.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider()
                Text("Merge adds missing entries and records. Existing local records win if identifiers conflict; counts are not added together, avoiding duplicates.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { runTask {
                    val count = manager.merge(payload)
                    pending = null
                    message = "Merge complete. $count new entries added; missing Dhikr phrases and dated records were imported where available. Existing data was preserved."
                } }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Merge with my data") }
                OutlinedButton(onClick = { confirmReplace = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Replace local data…") }
                TextButton(onClick = { pending = null }, modifier = Modifier.fillMaxWidth()) { Text("Cancel import") }
            } }
        }
        item { Text("A backup does not automatically migrate the original NUR V2 app. Import only NUR-M3 JSON exports. Keep your existing installation until you have verified the exported file.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    if (confirmReplace) AlertDialog(
        onDismissRequest = { confirmReplace = false },
        title = { Text("Replace local data?") },
        text = { Text(if ((pending?.schema ?: 3) >= 3) "This replaces your saved entries, completion history, Dhikr phrases and dated counts with the selected backup. NUR preserves a complete local recovery snapshot first. Export your current data to a safe location before continuing. This cannot recover an uninstalled app." else "This older backup replaces your entries and completion history. Your current Dhikr counters will be kept because the backup contains no Dhikr data. A local recovery snapshot is preserved first. Export your current data before continuing.") },
        confirmButton = { TextButton(onClick = {
            confirmReplace = false
            val payload = pending ?: return@TextButton
            runTask {
                manager.replace(payload)
                pending = null
                recoveryAvailable = true
                message = "Restore complete. Your previous data is available as a local recovery snapshot."
            }
        }) { Text("Replace data") } },
        dismissButton = { TextButton(onClick = { confirmReplace = false }) { Text("Cancel") } }
    )
}
