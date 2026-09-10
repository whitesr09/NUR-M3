package com.nshd.nurm3.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.ai.*
import com.nshd.nurm3.data.NurPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NurAiScreen(prefs: NurPreferences, model: NurViewModel) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val repo = remember(context) { NurAiRepository(context) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var messages by remember { mutableStateOf(repo.history()) }
    var input by rememberSaveable { mutableStateOf("") }
    var keyInput by remember { mutableStateOf("") }
    var modelInput by remember { mutableStateOf(repo.model()) }
    var selectedModel by remember { mutableStateOf(repo.model()) }
    var resolvedModel by remember { mutableStateOf("") }
    var availableModels by remember { mutableStateOf<List<AiModelOption>>(emptyList()) }
    var hasKey by remember { mutableStateOf(repo.hasKey()) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    var grounding by rememberSaveable { mutableStateOf(false) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var clearDialog by remember { mutableStateOf("") }
    var sources by remember { mutableStateOf<List<AiSource>>(emptyList()) }
    var streamingId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(hasKey, prefs.nurAiEnabled) {
        if (!hasKey || !prefs.nurAiEnabled) settingsOpen = true
    }
    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) {
            if (prefs.reduceMotion) listState.scrollToItem(messages.lastIndex)
            else listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)) {
                Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(Modifier.weight(1f)) {
                Text("NUR AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        busy -> "Thinking…"
                        resolvedModel.isNotBlank() -> "Ready · $resolvedModel"
                        selectedModel == NurAiRepository.AUTO_MODEL -> "Ready · automatic model"
                        else -> "Ready · $selectedModel"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { clearDialog = "history" }, enabled = messages.isNotEmpty() && !busy) {
                Icon(Icons.Default.EditSquare, contentDescription = "New chat")
            }
            IconButton(onClick = { settingsOpen = true }) {
                Icon(Icons.Default.Tune, contentDescription = "NUR AI settings")
            }
        }

        if (!hasKey || !prefs.nurAiEnabled) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Key, null)
                    Text(
                        if (!hasKey) "Add your Gemini API key to start chatting." else "Enable NUR AI network access to send messages.",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = { settingsOpen = true }) { Text("Set up") }
                }
            }
        }

        if (messages.isEmpty()) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                    Box(Modifier.size(74.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("Assalamu alaikum", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Ask about Islamic learning, build a routine, understand a concept or reflect on your day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(18.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Help me plan my prayers today",
                        "Explain a Quran concept simply",
                        "Give me a focused study routine",
                        "Suggest a short dhikr routine"
                    ).forEach { suggestion ->
                        SuggestionChip(onClick = { input = suggestion }, label = { Text(suggestion) })
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    AiBubble27(
                        message = message,
                        typing = message.id == streamingId && message.text.isBlank(),
                        streaming = message.id == streamingId && message.text.isNotBlank(),
                        onCopy = { clipboard.setText(AnnotatedString(message.text)) }
                    )
                }
                if (sources.isNotEmpty()) {
                    item {
                        NurPanel(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary)
                                Text("Retrieved sources", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            }
                            Text("Links below came from Gemini grounding metadata. Check the original source before relying on it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            sources.forEach { source ->
                                TextButton(onClick = {
                                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.url))) }
                                    catch (_: ActivityNotFoundException) { notice = "No browser is available." }
                                    catch (_: SecurityException) { notice = "The source could not be opened." }
                                }) {
                                    Icon(Icons.Default.OpenInNew, null, Modifier.size(17.dp))
                                    Spacer(Modifier.width(7.dp))
                                    Text(source.title.take(100), maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = if (LocalNurGlass.current.enabled) 0.72f else 1f),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (notice.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text(notice, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        IconButton(onClick = { notice = "" }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, "Dismiss", Modifier.size(16.dp)) }
                    }
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it.take(8000) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message NUR AI") },
                        maxLines = 5,
                        enabled = !busy,
                        shape = RoundedCornerShape(26.dp),
                        trailingIcon = if (input.isNotBlank() && !busy) ({
                            IconButton(onClick = { input = "" }) { Icon(Icons.Default.Close, "Clear message") }
                        }) else null
                    )
                    FilledIconButton(
                        onClick = {
                            val text = input.trim()
                            if (text.isBlank() || busy || !hasKey || !prefs.nurAiEnabled) return@FilledIconButton
                            input = ""
                            notice = ""
                            sources = emptyList()
                            val request = messages + NurAiRepository.userMessage(text)
                            val draft = NurAiRepository.modelMessage("")
                            messages = request + draft
                            streamingId = draft.id
                            repo.saveHistory(request)
                            busy = true
                            scope.launch {
                                try {
                                    val reply = repo.sendStreaming(request, prefs.nurAiEnabled, grounding) { partial ->
                                        withContext(Dispatchers.Main.immediate) {
                                            messages = request + draft.copy(text = partial)
                                        }
                                    }
                                    messages = request + draft.copy(text = reply.text)
                                    sources = reply.sources
                                    resolvedModel = reply.model
                                    repo.saveHistory(messages)
                                } catch (cancelled: CancellationException) {
                                    messages = request
                                    repo.saveHistory(request)
                                    throw cancelled
                                } catch (failure: Exception) {
                                    messages = request
                                    repo.saveHistory(request)
                                    notice = failure.message ?: "NUR AI could not answer. Tap send again in a moment."
                                } finally {
                                    streamingId = null
                                    busy = false
                                }
                            }
                        },
                        enabled = input.isNotBlank() && !busy && hasKey && prefs.nurAiEnabled,
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape
                    ) {
                        Icon(if (busy) Icons.Default.MoreHoriz else Icons.Default.ArrowUpward, contentDescription = "Send message")
                    }
                }
                Text(
                    "NUR AI can make mistakes. Verify Quran and Hadith references for important religious questions.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (settingsOpen) {
        ModalBottomSheet(onDismissRequest = { settingsOpen = false }) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("NUR AI settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Your key, model preference and local chat history stay in NUR private storage. Messages are sent only when you submit them.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                NurSettingRow("Enable network access", "Allow explicit NUR AI requests to Gemini", prefs.nurAiEnabled) { model.setting("nur_ai", it) }

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it.take(512) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (hasKey) "Replace API key" else "Gemini API key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                Button(
                    onClick = {
                        runCatching {
                            repo.saveKey(keyInput.trim())
                            keyInput = ""
                            hasKey = true
                            resolvedModel = ""
                            availableModels = emptyList()
                            notice = "API key saved securely on this device."
                        }.onFailure { notice = "Could not save the API key." }
                    },
                    enabled = keyInput.isNotBlank() && !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Icon(Icons.Default.Key, null); Spacer(Modifier.width(8.dp)); Text("Save API key") }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            notice = ""
                            try {
                                availableModels = repo.discoverModels(prefs.nurAiEnabled)
                                notice = if (availableModels.isEmpty()) "No compatible chat models were returned for this key."
                                else "Ready · ${availableModels.size} compatible models found."
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (failure: Exception) {
                                notice = failure.message ?: "Could not test the Gemini connection."
                            } finally { busy = false }
                        }
                    },
                    enabled = hasKey && prefs.nurAiEnabled && !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Icon(Icons.Default.WifiTethering, null); Spacer(Modifier.width(8.dp)); Text("Test connection & find models") }

                Text("Model", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Auto is recommended. NUR can rotate to another compatible model when one is retired, busy or temporarily unavailable.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NurAiRepository.MODEL_PRESETS.forEach { preset ->
                        FilterChip(
                            selected = modelInput == preset,
                            onClick = { modelInput = preset },
                            label = { Text(if (preset == NurAiRepository.AUTO_MODEL) "Auto" else preset) }
                        )
                    }
                }
                if (availableModels.isNotEmpty()) {
                    Text("Available to this key", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableModels.take(10).forEach { option ->
                            SuggestionChip(onClick = { modelInput = option.id }, label = { Text(option.id) })
                        }
                    }
                }
                Button(
                    onClick = {
                        runCatching {
                            repo.saveModel(modelInput)
                            selectedModel = repo.model()
                            modelInput = selectedModel
                            resolvedModel = ""
                            notice = if (selectedModel == NurAiRepository.AUTO_MODEL) "Automatic model selection enabled." else "Model preference saved."
                        }.onFailure { notice = it.message ?: "Invalid model ID." }
                    },
                    enabled = !busy && NurAiRepository.validModel(modelInput.trim()),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save model") }

                NurSettingRow("Web grounding", "Ask Gemini for retrieval metadata and source links when available", grounding) { grounding = it }
                OutlinedButton(onClick = {
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))) }
                    catch (_: ActivityNotFoundException) { notice = "No browser is available." }
                    catch (_: SecurityException) { notice = "The browser could not be opened." }
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.OpenInNew, null); Spacer(Modifier.width(8.dp)); Text("Open Google AI Studio")
                }
                TextButton(onClick = { clearDialog = "key" }, enabled = hasKey && !busy) { Text("Remove saved API key") }
                TextButton(onClick = { clearDialog = "history" }, enabled = !busy) { Text("Clear chat history") }
                TextButton(onClick = { clearDialog = "all" }, enabled = !busy) { Text("Clear all AI data") }
                Spacer(Modifier.height(22.dp))
            }
        }
    }

    if (clearDialog.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { clearDialog = "" },
            title = { Text(if (clearDialog == "history") "Start a new chat?" else "Delete AI data?") },
            text = { Text(when (clearDialog) {
                "key" -> "Remove the saved Gemini API key from this device?"
                "history" -> "Clear the current local NUR AI conversation?"
                else -> "Delete local AI history, API key and model preference, then disable NUR AI network access?"
            }) },
            confirmButton = {
                TextButton(onClick = {
                    when (clearDialog) {
                        "key" -> { repo.clearKey(); hasKey = false }
                        "history" -> { repo.clearHistory(); messages = emptyList(); sources = emptyList(); resolvedModel = "" }
                        else -> {
                            repo.clearAll(); hasKey = false; messages = emptyList(); sources = emptyList(); resolvedModel = ""
                            selectedModel = repo.model(); modelInput = selectedModel; model.setting("nur_ai", false)
                        }
                    }
                    clearDialog = ""
                }, enabled = !busy) { Text(if (clearDialog == "history") "New chat" else "Delete") }
            },
            dismissButton = { TextButton(onClick = { clearDialog = "" }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AiBubble27(message: AiMessage, typing: Boolean, streaming: Boolean, onCopy: () -> Unit) {
    val user = message.role == "user"
    val bubble = if (user) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (user) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(
        topStart = 22.dp,
        topEnd = 22.dp,
        bottomStart = if (user) 22.dp else 6.dp,
        bottomEnd = if (user) 6.dp else 22.dp
    )
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (user) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!user) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), modifier = Modifier.size(28.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AutoAwesome, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary) }
            }
            Spacer(Modifier.width(7.dp))
        }
        Surface(
            modifier = Modifier.fillMaxWidth(0.84f),
            shape = shape,
            color = bubble.copy(alpha = if (LocalNurGlass.current.enabled && !user) 0.76f else 1f),
            border = if (!user) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)) else null
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (typing) {
                    AiTypingIndicator27()
                } else {
                    Text(message.text, style = MaterialTheme.typography.bodyMedium, color = content)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            Instant.ofEpochMilli(message.time).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("h:mm a")),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            color = content.copy(alpha = 0.62f)
                        )
                        if (streaming) Text("typing…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        if (!user && message.text.isNotBlank() && !streaming) {
                            IconButton(onClick = onCopy, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.ContentCopy, "Copy answer", Modifier.size(15.dp), tint = content.copy(alpha = 0.72f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiTypingIndicator27() {
    val transition = rememberInfiniteTransition(label = "NUR AI thinking")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "Thinking dots"
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(3) { index ->
            val active = phase.toInt().coerceIn(0, 2) == index
            Box(
                Modifier.size(if (active) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = if (active) 1f else 0.32f))
            )
        }
        Spacer(Modifier.width(4.dp))
        Text("Thinking", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
