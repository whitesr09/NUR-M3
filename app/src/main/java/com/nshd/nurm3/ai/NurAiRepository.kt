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
 * User-owned Gemini client. Nothing is sent until send/sendStreaming is explicitly invoked
 * after the caller supplies consent. Credentials, model choice and chat history live in the
 * Keystore-backed private store, which is excluded from NUR backups.
 */
data class AiMessage(val id: String, val role: String, val text: String, val time: Long = System.currentTimeMillis())
data class AiSource(val title: String, val url: String)
data class AiReply(val text: String, val sources: List<AiSource> = emptyList())

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
        require(validModel(normalized)) { "Use a valid Gemini model ID." }
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

    private fun openConnection(endpoint: String, key: String, accept: String): HttpURLConnection =
        (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = false
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", accept)
            setRequestProperty("x-goog-api-key", key)
        }

    private fun httpError(code: Int): String = when (code) {
        400 -> "The request was rejected. Check your model and settings."
        401, 403 -> "Gemini authentication failed. Check your API key and project access."
        404 -> "The selected model is unavailable. Choose another Gemini model."
        429 -> "Gemini rate limit reached. Try again later or check your quota."
        else -> "Gemini request failed (HTTP $code)."
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

    /**
     * Streams Gemini Server-Sent Events and emits the aggregated readable text after each chunk.
     * The callback is suspend so the UI can safely switch to its main dispatcher before updating state.
     */
    suspend fun sendStreaming(
        messages: List<AiMessage>,
        consent: Boolean,
        useGrounding: Boolean = false,
        onPartial: suspend (String) -> Unit = {}
    ): AiReply = withContext(Dispatchers.IO) {
        require(consent) { "Enable NUR AI network access first." }
        val key = store.get(keyName) ?: error("Add your Gemini API key first.")
        val selectedModel = model()
        val body = requestBody(messages, useGrounding)
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$selectedModel:streamGenerateContent?alt=sse"
        val connection = openConnection(endpoint, key, "text/event-stream")
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            if (code !in 200..299) error(httpError(code))

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
                        // Comments and unknown SSE fields are deliberately ignored.
                    }
                }
                consumeEvent()
            }
            if (aggregate.isBlank()) error("Gemini returned no readable text. The request may have been blocked.")
            AiReply(aggregate.toString(), sources.take(12))
        } finally {
            connection.disconnect()
        }
    }

    /** Non-streaming caller compatibility; the streamed transport is still used underneath. */
    suspend fun send(messages: List<AiMessage>, consent: Boolean, useGrounding: Boolean = false): AiReply =
        sendStreaming(messages, consent, useGrounding)

    companion object {
        const val DEFAULT_MODEL = "gemini-2.5-flash"
        val MODEL_PRESETS = listOf("gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-flash-latest")
        private const val MAX_STREAM_CHARS = 1_048_576
        private const val MAX_TEXT_CHARS = 32_000
        private val MODEL_PATTERN = Regex("[A-Za-z0-9._-]{3,80}")
        fun validModel(value: String) = MODEL_PATTERN.matches(value)
        fun userMessage(text: String) = AiMessage(UUID.randomUUID().toString(), "user", text)
        fun modelMessage(text: String) = AiMessage(UUID.randomUUID().toString(), "model", text)
    }
}
