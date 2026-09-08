package com.nshd.nurm3.ui

import android.app.KeyguardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.data.NurLock
import kotlinx.coroutines.delay

@Composable
fun LockScreen(lock: NurLock, authenticate: ((Boolean, String) -> Unit) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var remaining by remember { mutableLongStateOf(lock.remainingLockoutMs()) }
    val context = LocalContext.current
    val deviceSecure = remember(context) { context.getSystemService(KeyguardManager::class.java).isDeviceSecure }
    LaunchedEffect(lock) {
        while (true) { remaining = lock.remainingLockoutMs(); delay(1000) }
    }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(20.dp))
        Text("نُور", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Text("Welcome back", style = MaterialTheme.typography.headlineSmall)
        Text("Unlock your private Journey", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = pin, onValueChange = { if (it.length <= 12 && it.all(Char::isDigit)) pin = it }, label = { Text("App PIN") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true, modifier = Modifier.fillMaxWidth())
        if (remaining > 0L) Text("Try again in ${(remaining + 999) / 1000} seconds", color = MaterialTheme.colorScheme.error)
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            val entered = pin
            pin = ""
            if (lock.unlock(entered)) error = ""
            else { error = "Incorrect PIN"; remaining = lock.remainingLockoutMs() }
        }, enabled = pin.length >= 6 && remaining == 0L, modifier = Modifier.fillMaxWidth()) { Text("Unlock") }
        if (deviceSecure) TextButton(onClick = { authenticate { success, message ->
            if (success) { lock.unlockAfterDeviceAuthentication(); error = "" }
            else if (message.isNotBlank()) error = message
        } }) { Text("Use device screen lock") }
        Text("Your records are stored locally. This lock protects access to NUR; it is not full-database encryption.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PrivacyScreen(lock: NurLock, privatePreview: Boolean, onPreviewChange: (Boolean) -> Unit) {
    val configured by lock.configuredFlow.collectAsState()
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var remove by remember { mutableStateOf(false) }
    fun clearFields() { current = ""; next = ""; confirm = "" }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Privacy & app lock", style = MaterialTheme.typography.headlineMedium)
            Text("Protect the app with a PIN. Your Android device credential can also unlock NUR when a secure screen lock is configured.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { SettingToggle("Private previews", "Block screenshots and hide NUR in the recent-apps preview", privatePreview, onChange = onPreviewChange) }
        item { SectionCard(if (configured) "Change app PIN" else "Set up app PIN", "Use 6–12 digits. A PIN is required before app lock can be enabled.", null) {
            if (configured) OutlinedTextField(value = current, onValueChange = { if (it.length <= 12 && it.all(Char::isDigit)) current = it }, label = { Text("Current PIN") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = next, onValueChange = { if (it.length <= 12 && it.all(Char::isDigit)) next = it }, label = { Text("New PIN") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = confirm, onValueChange = { if (it.length <= 12 && it.all(Char::isDigit)) confirm = it }, label = { Text("Confirm new PIN") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                val result = runCatching { lock.setPin(next, if (configured) current else null) }
                message = if (result.isSuccess) "App lock is enabled. It will lock when NUR leaves the foreground." else result.exceptionOrNull()?.message ?: "Could not save PIN"
                if (result.isSuccess) clearFields()
            }, enabled = next.length in 6..12 && next == confirm && (!configured || current.isNotBlank()), modifier = Modifier.fillMaxWidth()) { Text(if (configured) "Change PIN" else "Enable app lock") }
            if (configured) {
                OutlinedButton(onClick = { lock.lock() }, modifier = Modifier.fillMaxWidth()) { Text("Lock NUR now") }
                TextButton(onClick = { remove = true }, modifier = Modifier.fillMaxWidth()) { Text("Remove app lock") }
            }
        } }
        if (message.isNotBlank()) item { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { SectionCard("Important", "Security and recovery", null) {
            Text("Your PIN and API credentials are excluded from backups. The database itself is not encrypted. If you forget your PIN, use your configured Android device credential to unlock. Without either credential, reinstalling may be necessary and can delete local data. Export backups before changing devices.", style = MaterialTheme.typography.bodySmall)
        } }
    }
    if (remove) AlertDialog(onDismissRequest = { remove = false }, title = { Text("Remove app lock?") }, text = { Text("Enter your current PIN in the field above. Removing the lock will allow anyone with access to your unlocked device to open NUR.") }, confirmButton = { TextButton(onClick = {
        val result = runCatching { lock.removePin(current) }
        message = if (result.isSuccess) "App lock removed." else result.exceptionOrNull()?.message ?: "Incorrect PIN"
        if (result.isSuccess) clearFields()
        remove = false
    }, enabled = current.isNotBlank()) { Text("Remove lock") } }, dismissButton = { TextButton(onClick = { remove = false }) { Text("Cancel") } })
}
