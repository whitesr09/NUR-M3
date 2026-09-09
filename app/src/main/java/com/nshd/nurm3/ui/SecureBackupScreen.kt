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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun SecureBackupScreen() {
    val context = LocalContext.current
    val manager = remember(context) { SecureBackupManager(context) }
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<SecureSnapshot?>(null) }
    var confirmReplace by remember { mutableStateOf(false) }
    var includeFocusWarning by remember { mutableStateOf(false) }
    fun runTask(block: suspend () -> Unit) {
        if (busy) return
        scope.launch {
            busy = true; message = ""
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { message = error.message ?: "The operation could not be completed. Existing records were not intentionally deleted." }
            finally { busy = false }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) runTask {
            val pass = password.toCharArray()
            try {
                val encrypted = manager.export(pass)
                manager.write(uri, encrypted)
                message = "Encrypted backup saved. Keep your passphrase and file in separate safe places."
                password = ""; confirmation = ""
            } finally { pass.fill('\u0000') }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) runTask {
            val pass = password.toCharArray()
            try {
                pending = manager.preview(manager.read(uri), pass)
                message = "Integrity verified. Review the contents before importing."
                password = ""; confirmation = ""
            } finally { pass.fill('\u0000') }
        }
    }
    LazyColumn(contentPadding = PaddingValues(NurDesign.pagePadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { NurPageHeading("Privacy & recovery", "Encrypted backup", "Protect your portable records with a passphrase you control.") }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Text("What is included", style = MaterialTheme.typography.titleMedium)
                Text("Prayer, Amanah, Muhasaba and Rhythm definitions and history, Dhikr records, and Focus sessions and routines. API keys, AI conversations, app PINs and private journal content are not included. This is not a full-device backup.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("The existing unencrypted JSON format remains available separately for legacy imports. Use this encrypted format for private exports.", style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Text("Passphrase", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(password, { password = it.take(256) }, label = { Text("Backup passphrase") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(confirmation, { confirmation = it.take(256) }, label = { Text("Confirm for a new export") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Use at least 12 characters. NUR cannot recover a forgotten passphrase. It is never saved with the backup.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { exportLauncher.launch("NUR-M3-${LocalDate.now()}.nur.json") }, enabled = !busy && password.length >= 12 && password == confirmation, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Lock, null); Spacer(Modifier.width(8.dp)); Text("Export encrypted backup") }
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }, enabled = !busy && password.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.FileOpen, null); Spacer(Modifier.width(8.dp)); Text("Open encrypted backup") }
            }
        }
        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Processing locally…", style = MaterialTheme.typography.bodySmall) }
        if (message.isNotBlank()) item { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        pending?.let { snapshot ->
            item {
                NurPanel(Modifier.fillMaxWidth()) {
                    Text("Verified import preview", style = MaterialTheme.typography.titleMedium)
                    Text("Fingerprint: ${snapshot.fingerprint.take(16)}…", style = MaterialTheme.typography.labelSmall)
                    Text("${snapshot.core.entries.size} entries · ${snapshot.core.completions.size} completions")
                    Text("${snapshot.core.dhikrPhrases.size} Dhikr phrases · ${snapshot.core.dhikrDays.size} dated counts")
                    Text("${snapshot.sessions.size} Focus sessions · ${snapshot.routines.size} routines")
                    HorizontalDivider()
                    Text("Merge adds missing identifiers. Existing local records win; counts are never added together.", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { runTask { val added = manager.merge(snapshot); pending = null; message = "Merge finished. $added new core entries added. Existing identifiers were preserved." } }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Merge with my data") }
                    OutlinedButton(onClick = { confirmReplace = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Replace local records…") }
                    TextButton(onClick = { pending = null }, modifier = Modifier.fillMaxWidth()) { Text("Cancel import") }
                }
            }
        }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Text("Data integrity", style = MaterialTheme.typography.titleMedium)
                Text("Encrypted imports authenticate the complete payload before parsing. Existing local records are not modified during preview. Replacement creates local recovery snapshots first.", style = MaterialTheme.typography.bodySmall)
                Text("Core and Focus use separate databases, so a replacement cannot be atomic across both. If a later stage fails, use the recovery snapshots or your external backup before trying again.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Your private journal and AI credentials remain excluded. App lock protects access to NUR but does not encrypt the main Room database.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (confirmReplace) AlertDialog(onDismissRequest = { confirmReplace = false }, title = { Text("Replace local records?") }, text = { Text("This replaces the included core and Focus records after preserving recovery snapshots. Existing records not present in the backup will be removed. Private journal and AI data are not included. Export your current data first. The two databases are not one atomic transaction.") }, confirmButton = { TextButton(onClick = {
        val snapshot = pending ?: return@TextButton
        confirmReplace = false
        runTask { manager.replace(snapshot); pending = null; message = "Replacement finished. Local recovery snapshots were preserved." }
    }, enabled = !busy) { Text("Replace records") } }, dismissButton = { TextButton(onClick = { confirmReplace = false }) { Text("Cancel") } })
}
