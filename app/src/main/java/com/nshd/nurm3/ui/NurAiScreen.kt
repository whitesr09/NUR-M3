package com.nshd.nurm3.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.ai.*
import com.nshd.nurm3.data.NurPreferences
import com.nshd.nurm3.NurViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun NurAiScreen(prefs: NurPreferences, model: NurViewModel) {
    val context = LocalContext.current
    val repo = remember(context) { NurAiRepository(context) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var messages by remember { mutableStateOf(repo.history()) }
    var input by rememberSaveable { mutableStateOf("") }
    var keyInput by remember { mutableStateOf("") }
    var hasKey by remember { mutableStateOf(repo.hasKey()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var grounding by rememberSaveable { mutableStateOf(false) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var clearDialog by remember { mutableStateOf("") }
    var sources by remember { mutableStateOf<List<AiSource>>(emptyList()) }
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("NUR AI", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = { settingsOpen = !settingsOpen }) { Icon(Icons.Default.Settings, "AI settings") }
        }
        if (settingsOpen || !prefs.nurAiEnabled || !hasKey) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Optional Gemini companion", style = MaterialTheme.typography.titleMedium)
                Text("Messages are sent to Google only when you submit them with network access enabled. Your API key stays in Android's private Keystore-backed storage. Google's API data and billing policies still apply.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                NurSettingRow("Enable NUR AI network access", "Allow explicit chat requests to Gemini", prefs.nurAiEnabled) { model.setting("nur_ai", it) }
                OutlinedTextField(keyInput, { keyInput = it.take(512) }, modifier = Modifier.fillMaxWidth(), label = { Text(if (hasKey) "Replace API key" else "Gemini API key") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                OutlinedButton(onClick = { runCatching { repo.saveKey(keyInput.trim()); keyInput = ""; hasKey = true; error = "Key saved securely." }.onFailure { error = "Could not save the key." } }, enabled = keyInput.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Save API key") }
                TextButton(onClick = { clearDialog = "key" }, enabled = hasKey) { Text("Remove saved API key") }
                NurSettingRow("Web grounding", "Optional source retrieval may use extra API quota. Off by default.", grounding) { grounding = it }
                Text("Create or review your key in Google AI Studio. Never share it in chat or commit it to GitHub.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))) }) { Text("Open Google AI Studio") }
                TextButton(onClick = { clearDialog = "history" }) { Text("Clear chat history") }
                TextButton(onClick = { clearDialog = "all" }) { Text("Clear all AI data") }
                HorizontalDivider()
            }
        }
        if (messages.isEmpty()) {
            Column(Modifier.weight(1f).padding(24.dp), verticalArrangement = Arrangement.Center) {
                NurEmptyState(Icons.Default.AutoAwesome, "Ask with curiosity", "Explore Islamic learning, plan a routine, or reflect on a goal. AI answers can be mistaken and are not verified religious rulings.")
            }
        } else {
            LazyColumn(Modifier.weight(1f), state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(messages, key = { it.id }) { message ->
                    NurPanel(Modifier.fillMaxWidth()) {
                        Text(if (message.role == "user") "YOU" else "NUR AI · UNVERIFIED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(message.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (sources.isNotEmpty()) item {
                    NurPanel(Modifier.fillMaxWidth()) {
                        Text("Retrieved sources", style = MaterialTheme.typography.titleSmall)
                        Text("These links were returned by grounding. Review the original context and attribution before relying on them.", style = MaterialTheme.typography.bodySmall)
                        sources.forEach { source ->
                            TextButton(onClick = { try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.url))) } catch (_: ActivityNotFoundException) { error = "No browser is available." } }) { Icon(Icons.Default.OpenInNew, null); Spacer(Modifier.width(6.dp)); Text(source.title.take(100)) }
                        }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().imePadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("AI can make mistakes. Verify Quran/Hadith references and consult a qualified scholar for personal rulings.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(input, { input = it.take(8000) }, modifier = Modifier.weight(1f), placeholder = { Text("Ask NUR AI…") }, maxLines = 4, enabled = !busy)
                IconButton(onClick = {
                    val text = input.trim()
                    if (text.isBlank() || busy) return@IconButton
                    input = ""; error = ""; sources = emptyList()
                    val request = messages + NurAiRepository.userMessage(text)
                    messages = request
                    repo.saveHistory(request)
                    busy = true
                    scope.launch {
                        try {
                            val reply = repo.send(request, prefs.nurAiEnabled, grounding)
                            messages = request + NurAiRepository.modelMessage(reply.text)
                            sources = reply.sources
                            repo.saveHistory(messages)
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (failure: Exception) { error = failure.message ?: "The request failed. Your message is still saved." }
                        finally { busy = false }
                    }
                }, enabled = input.isNotBlank() && !busy && prefs.nurAiEnabled && hasKey) { Icon(Icons.Default.Send, "Send message") }
            }
        }
    }
    if (clearDialog.isNotBlank()) AlertDialog(onDismissRequest = { clearDialog = "" }, title = { Text("Delete AI data?") }, text = { Text(when (clearDialog) { "key" -> "Remove your saved API key from NUR?"; "history" -> "Permanently delete the local chat history?"; else -> "Delete the local chat history and API key, and disable AI network access?" }) }, confirmButton = { TextButton(onClick = {
        when (clearDialog) {
            "key" -> { repo.clearKey(); hasKey = false }
            "history" -> { repo.clearHistory(); messages = emptyList(); sources = emptyList() }
            else -> { repo.clearAll(); hasKey = false; messages = emptyList(); sources = emptyList(); model.setting("nur_ai", false) }
        }
        clearDialog = ""
    }, enabled = !busy) { Text("Delete") } }, dismissButton = { TextButton(onClick = { clearDialog = "" }) { Text("Cancel") } })
}
