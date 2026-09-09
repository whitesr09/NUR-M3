package com.nshd.nurm3.ai

import android.content.Context
import com.nshd.nurm3.data.NurPrivateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * User-owned Gemini client. Nothing is sent until the user explicitly enables NUR AI
 * networking and submits a request. Credentials, model preference and local history live
 * only in NUR's Keystore-backed private store and are excluded from backups/widgets.
 */
data class AiMessage(val id: String, val role: String, val text: String, val time: Long = System.currentTimeMillis())
data class AiSource(val title: String, val url: String)
data class AiReply(val text: String, val sources: List<AiSource> = emptyList(), val model: String = "")
data class AiModelOption(val id: String, val displayName: String)

private class ModelUnavailableException(message: String) : Exception(message)

class NurAiRepository(context: Context) {
    private val store = NurPrivateStore(context)
    private val keyName = "nur_ai_api_key"
    private val historyName = "nur_ai_history"
    private val modelName = "nur_ai_model"

    fun hasKey() = store.contains(keyName)
    fun saveKey(value: String) {
        require(value.isNotBlank() && value.length <= 512 && value.none { it.isWhitespace() })
        store.put(keyName, value)
    }
    fun clearKey() = store.clear(keyName)

    fun model(): String = store.get(modelName)?.takeIf(::validModel) ?: DEFAULT_MODEL
    fun saveModel(value: String) {
        val normalized = value.trim()
        require(validModel(normalized)) { "Use Auto or a valid Gemini model ID." }
        store.put(modelName, normalized)
    }
    fun resetModel() = store.clear(modelName)

    fun history(): List<AiMessage> = runCatching {
        val raw = store.get(historyName) ?: return@runCatching emptyList()
        val array = JSONArray(raw)
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            AiMessage(item.getString("id"), item.getString("role"), item.getString("text"), item.getLong("time"))
        }.filter { it.role in setOf("user", "model") && it.text.length <= 32_000 }
    }.getOrDefault(emptyList())

    fun saveHistory(messages: List<AiMessage>) {
        val array = JSONArray()
        messages.takeLast(100).forEach {
            require(it.role in setOf("user", "model"))
            require(it.text.length <= 32_000)
            array.put(JSONObject().put("id", it.id).put("role", it.role).put("text", it.text).put("time", it.time))
        }
        store.put(historyName, array.toString())
    }
    fun clearHistory() = store.clear(historyName)
    fun clearAll() { clearHistory(); clearKey(); resetModel() }

    private fun requestBody(messages: List<AiMessage>, useGrounding: Boolean): JSONObject {
        require(messages.isNotEmpty() && messages.last().role == "user")
        val contents = JSONArray()
        messages.takeLast(16).forEach { message ->
            require(message.role in setOf("user", "model"))
            require(message.text.isNotBlank() && message.text.length <= 8000)
            contents.put(
                JSONObject().put("role", message.role)
                    .put("parts", JSONArray().put(JSONObject().put("text", message.text)))
            )
        }
        val instruction = "You are NUR AI, a respectful Islamic learning and productivity companion. Explain clearly, distinguish verified Quran/Hadith text from your explanation, and never fabricate quotations, references, or religious rulings. If you cannot verify an exact reference, say so and recommend consulting a qualified scholar or the primary source. Acknowledge differences among recognized scholarly views without claiming universal authority. Do not diagnose mental health conditions. Do not claim to have changed the user's tasks or records."
        return JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", instruction))))
            .put("contents", contents)
            .put("generationConfig", JSONObject().put("maxOutputTokens", 2048).put("temperature", 0.35))
            .also { if (useGrounding) it.put("tools", JSONArray().put(JSONObject().put("google_search", JSONObject()))) }
    }

    private fun connection(endpoint: String, key: String, method: String, accept: String): HttpURLConnection =
        (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = false
            doOutput = method == "POST"
            setRequestProperty("Accept", accept)
            setRequestProperty("x-goog-api-key", key)
            if (method == "POST") setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }

    private fun serverMessage(connection: HttpURLConnection): String = runCatching {
        val stream = connection.errorStream ?: return@runCatching ""
        val raw = stream.bufferedReader(Charsets.UTF_8).use { it.readText().take(16_000) }
        val root = JSONObject(raw)
        root.optJSONObject("error")?.optString("message").orEmpty().replace(Regex("\\s+"), " ").take(260)
    }.getOrDefault("")

    private fun requestFailure(connection: HttpURLConnection, code: Int): Exception {
        val detail = serverMessage(connection)
        val base = when (code) {
            400 -> "Gemini rejected the request."
            401, 403 -> "Gemini rejected this API key or the key does not have Gemini API access. Create or review a current Gemini API key in Google AI Studio."
            404 -> "The selected Gemini model is no longer available."
            429 -> "Gemini rate limit reached. Try again later or review your quota."
            else -> "Gemini request failed (HTTP $code)."
        }
        val message = if (detail.isBlank()) base else "$base $detail"
        return if (code == 404) ModelUnavailableException(message) else IllegalStateException(message)
    }

    private fun modelScore(id: String): Int = when {
        id == "gemini-3.5-flash" -> 0
        id == "gemini-flash-latest" -> 1
        id.contains("flash", ignoreCase = true) && !id.contains("preview", ignoreCase = true) -> 2
        id.contains("flash", ignoreCase = true) -> 3
        id.contains("pro", ignoreCase = true) && !id.contains("preview", ignoreCase = true) -> 4
        id.startsWith("gemini-", ignoreCase = true) -> 5
        else -> 20
    }

    private fun listModels(key: String): List<AiModelOption> {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models?pageSize=1000"
        val connection = connection(endpoint, key, "GET", "application/json")
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw requestFailure(connection, code)
            val text = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText().take(MAX_MODEL_LIST_CHARS) }
            val array = JSONObject(text).optJSONArray("models") ?: JSONArray()
            val result = mutableListOf<AiModelOption>()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val name = item.optString("name").removePrefix("models/")
                if (!validModel(name) || !name.startsWith("gemini-", ignoreCase = true)) continue
                val methods = item.optJSONArray("supportedGenerationMethods") ?: JSONArray()
                var supportsGenerate = false
                for (m in 0 until methods.length()) if (methods.optString(m) == "generateContent") supportsGenerate = true
                if (!supportsGenerate) continue
                val display = item.optString("displayName").ifBlank { name }
                result += AiModelOption(name, display.take(120))
            }
            return result.distinctBy { it.id }.sortedWith(compareBy<AiModelOption> { modelScore(it.id) }.thenBy { it.id })
        } finally {
            connection.disconnect()
        }
    }

    suspend fun discoverModels(consent: Boolean): List<AiModelOption> = withContext(Dispatchers.IO) {
        require(consent) { "Enable NUR AI network access first." }
        val key = store.get(keyName) ?: error("Add your Gemini API key first.")
        listModels(key)
    }

    private fun candidateText(candidate: JSONObject): String {
        val parts = candidate.optJSONObject("content")?.optJSONArray("parts") ?: return ""
        return buildString {
            for (i in 0 until parts.length()) {
                val value = parts.optJSONObject(i)?.optString("text").orEmpty()
                if (value.isNotBlank()) append(value)
            }
        }
    }

    private fun addSources(candidate: JSONObject, target: MutableList<AiSource>) {
        val chunks = candidate.optJSONObject("groundingMetadata")?.optJSONArray("groundingChunks") ?: return
        for (i in 0 until chunks.length()) {
            val web = chunks.optJSONObject(i)?.optJSONObject("web") ?: continue
            val uri = web.optString("uri")
            if (uri.startsWith("https://") && target.none { it.url == uri }) {
                target += AiSource(web.optString("title").ifBlank { "Retrieved source" }.take(160), uri)
            }
        }
    }

    private suspend fun sendStreamingWithModel(
        key: String,
        modelId: String,
        messages: List<AiMessage>,
        useGrounding: Boolean,
        onPartial: suspend (String) -> Unit
    ): AiReply {
        val body = requestBody(messages, useGrounding)
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:streamGenerateContent?alt=sse"
        val connection = connection(endpoint, key, "POST", "text/event-stream")
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            if (code !in 200..299) throw requestFailure(connection, code)

            val aggregate = StringBuilder()
            val sources = mutableListOf<AiSource>()
            var rawChars = 0
            val eventData = StringBuilder()

            suspend fun consumeEvent() {
                if (eventData.isEmpty()) return
                val payload = eventData.toString().trim()
                eventData.clear()
                if (payload.isEmpty() || payload == "[DONE]") return
                val root = runCatching { JSONObject(payload) }.getOrElse { error("Gemini returned an invalid stream event.") }
                root.optJSONObject("error")?.let { error(it.optString("message").ifBlank { "Gemini returned an error." }) }
                val candidate = root.optJSONArray("candidates")?.optJSONObject(0) ?: return
                val chunk = candidateText(candidate)
                if (chunk.isNotEmpty()) {
                    require(aggregate.length + chunk.length <= MAX_TEXT_CHARS) { "Response is too large." }
                    aggregate.append(chunk)
                    onPartial(aggregate.toString())
                }
                addSources(candidate, sources)
            }

            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    rawChars += line.length + 1
                    require(rawChars <= MAX_STREAM_CHARS) { "Response stream is too large." }
                    when {
                        line.isEmpty() -> consumeEvent()
                        line.startsWith("data:") -> {
                            if (eventData.isNotEmpty()) eventData.append('\n')
                            eventData.append(line.substring(5).trimStart())
                        }
                    }
                }
                consumeEvent()
            }
            if (aggregate.isBlank()) error("Gemini returned no readable text. The request may have been blocked.")
            return AiReply(aggregate.toString(), sources.take(12), modelId)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Streams Gemini responses. Auto mode first asks Gemini which generateContent models are
     * actually available to this key. If a manually selected model has been retired, NUR
     * automatically discovers a compatible fallback and retries once instead of trapping the
     * user on an obsolete model ID.
     */
    suspend fun sendStreaming(
        messages: List<AiMessage>,
        consent: Boolean,
        useGrounding: Boolean = false,
        onPartial: suspend (String) -> Unit = {}
    ): AiReply = withContext(Dispatchers.IO) {
        require(consent) { "Enable NUR AI network access first." }
        val key = store.get(keyName) ?: error("Add your Gemini API key first.")
        val selected = model()
        val discovered = if (selected == AUTO_MODEL) listModels(key) else emptyList()
        val first = if (selected == AUTO_MODEL) discovered.firstOrNull()?.id
            ?: error("No compatible Gemini chat model is available for this key.")
        else selected

        try {
            sendStreamingWithModel(key, first, messages, useGrounding, onPartial)
        } catch (unavailable: ModelUnavailableException) {
            val alternatives = if (discovered.isNotEmpty()) discovered else listModels(key)
            val fallback = alternatives.firstOrNull { it.id != first }?.id ?: throw unavailable
            sendStreamingWithModel(key, fallback, messages, useGrounding, onPartial)
        }
    }

    suspend fun send(messages: List<AiMessage>, consent: Boolean, useGrounding: Boolean = false): AiReply =
        sendStreaming(messages, consent, useGrounding)

    companion object {
        const val AUTO_MODEL = "auto"
        const val DEFAULT_MODEL = AUTO_MODEL
        val MODEL_PRESETS = listOf(AUTO_MODEL, "gemini-3.5-flash", "gemini-flash-latest")
        private const val MAX_STREAM_CHARS = 1_048_576
        private const val MAX_MODEL_LIST_CHARS = 1_048_576
        private const val MAX_TEXT_CHARS = 32_000
        private val MODEL_PATTERN = Regex("[A-Za-z0-9._-]{3,80}")
        fun validModel(value: String) = value == AUTO_MODEL || MODEL_PATTERN.matches(value)
        fun userMessage(text: String) = AiMessage(UUID.randomUUID().toString(), "user", text)
        fun modelMessage(text: String) = AiMessage(UUID.randomUUID().toString(), "model", text)
    }
}
