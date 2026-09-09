package com.nshd.nurm3.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.data.BackupHealth
import com.nshd.nurm3.data.BackupHealthChecker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Read-only diagnostics. Running this screen never creates, repairs or deletes user records. */
@Composable
fun BackupHealthScreen() {
    val context = LocalContext.current
    val checker = remember(context) { BackupHealthChecker(context) }
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf<BackupHealth?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    fun refresh() {
        if (busy) return
        scope.launch {
            busy = true
            message = ""
            try { report = checker.inspect() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { message = error.message ?: "Health checks could not be completed." }
            finally { busy = false }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    LazyColumn(contentPadding = PaddingValues(NurDesign.pagePadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { NurPageHeading("Privacy & recovery", "Backup health", "Check the local databases and recovery snapshots without changing your records.") }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text("Read-only verification", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("NUR runs SQLite quick checks and reports whether local recovery snapshots exist. It does not repair, replace or fabricate data.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedButton(onClick = ::refresh, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (busy) "Checking…" else "Run checks again")
                }
            }
        }

        report?.let { health ->
            item { HealthDatabaseCard("Daily Journey database", health.coreIntegrity, "${health.coreEntries} saved entry definitions") }
            item { HealthDatabaseCard("Focus database", health.focusIntegrity, "${health.focusSessions} saved focus sessions") }
            item {
                NurPanel(Modifier.fillMaxWidth()) {
                    Text("Recovery snapshots", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    RecoveryRow("Daily Journey recovery", health.coreRecovery)
                    RecoveryRow("Focus recovery", health.focusRecovery)
                    Text("A recovery snapshot normally appears after a replace operation. ‘Not present’ is not an error if you have never replaced local data.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                val allOk = health.coreIntegrity.equals("ok", true) && health.focusIntegrity.equals("ok", true)
                NurPanel(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(if (allOk) Icons.Default.VerifiedUser else Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text(if (allOk) "Local database checks passed" else "Review the reported checks", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(if (allOk) "Both databases returned SQLite quick_check = ok at the time of this scan." else "A non-ok or unavailable result does not automatically mean records are lost. Export a backup before troubleshooting or reinstalling.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (message.isNotBlank()) item { Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        item { Text("This development screen checks local storage only. It does not prove that an external backup file is current, decryptable or safely stored elsewhere.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun HealthDatabaseCard(title: String, result: String, detail: String) {
    val ok = result.equals("ok", true)
    NurPanel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(if (ok) Icons.Default.CheckCircle else Icons.Default.WarningAmber, contentDescription = null, tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(if (ok) "SQLite quick check: OK" else "SQLite quick check: $result", style = MaterialTheme.typography.bodySmall)
                Text(detail, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RecoveryRow(label: String, available: Boolean) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(if (available) Icons.Default.Lock else Icons.Default.RemoveCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(if (available) "Available" else "Not present", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
