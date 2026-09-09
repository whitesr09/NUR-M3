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

/** This client never sends data until send() is explicitly invoked after consent. */
data class AiMessage(val id: String, val role: String, val text: String, val time: Long = System.currentTimeMillis())
data class AiSource(val title: String, val url: String)
data class AiReply(val text: String, val sources: List<AiSource> = emptyList())

class NurAiRepository(context: Context) {
    private val store = NurPrivateStore(context)
    private val keyName = "nur_ai_api_key"
    private val historyName = "nur_ai_history"
    fun hasKey() = store.contains(keyName)
    fun saveKey(value: String) { require(value.isNotBlank() && value.length <= 512 && value.none { it.isWhitespace() }); store.put(keyName, value) }
    fun clearKey() = store.clear(keyName)
    fun history(): List<AiMessage> = runCatching {
        val raw = store.get(historyName) ?: return@runCatching emptyList()
        val array = JSONArray(raw)
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            AiMessage(item.getString("id"), item.getString("role"), item.getString("text"), item.getLong("time"))
        }
    }.getOrDefault(emptyList())
    fun saveHistory(messages: List<AiMessage>) {
        val array = JSONArray()
        messages.takeLast(100).forEach { array.put(JSONObject().put("id", it.id).put("role", it.role).put("text", it.text).put("time", it.time)) }
        store.put(historyName, array.toString())
    }
    fun clearHistory() = store.clear(historyName)
    fun clearAll() { clearHistory(); clearKey() }

    suspend fun send(messages: List<AiMessage>, consent: Boolean, useGrounding: Boolean = false): AiReply = withContext(Dispatchers.IO) {
        require(consent) { "Enable NUR AI network access first." }
        val key = store.get(keyName) ?: error("Add your Gemini API key first.")
        require(messages.isNotEmpty() && messages.last().role == "user")
        val contents = JSONArray()
        messages.takeLast(16).forEach { message ->
            require(message.text.length <= 8000)
            contents.put(JSONObject().put("role", if (message.role == "model") "model" else "user")
                .put("parts", JSONArray().put(JSONObject().put("text", message.text))))
        }
        val instruction = "You are NUR AI, a respectful Islamic learning and productivity companion. Explain clearly, distinguish verified Quran/Hadith text from your explanation, and never fabricate quotations, references, or religious rulings. If you cannot verify an exact reference, say so and recommend consulting a qualified scholar or the primary source. Acknowledge differences among recognized scholarly views without claiming universal authority. Do not diagnose mental health conditions. Do not claim to have changed the user's tasks or records."
        val body = JSONObject().put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", instruction))))
            .put("contents", contents)
            .put("generationConfig", JSONObject().put("maxOutputTokens", 2048).put("temperature", 0.35))
        if (useGrounding) body.put("tools", JSONArray().put(JSONObject().put("google_search", JSONObject())))
        val connection = (URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent").openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.instanceFollowRedirects = false
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("x-goog-api-key", key)
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            if (code !in 200..299) error(when (code) {
                400 -> "The request was rejected. Check your model and settings."
                401, 403 -> "Gemini authentication failed. Check your API key and project access."
                404 -> "The selected model is unavailable. Update NUR AI's model configuration."
                429 -> "Gemini rate limit reached. Try again later or check your quota."
                else -> "Gemini request failed (HTTP $code)."
            })
            val bytes = connection.inputStream.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    require(output.size() <= 1_048_576) { "Response is too large." }
                }
                output.toByteArray()
            }
            val root = JSONObject(String(bytes, Charsets.UTF_8))
            val candidate = root.optJSONArray("candidates")?.optJSONObject(0) ?: error("No response was returned. The request may have been blocked.")
            val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
            val text = buildString { if (parts != null) for (i in 0 until parts.length()) { val part = parts.optJSONObject(i) ?: continue; val value = part.optString("text"); if (value.isNotBlank()) { if (isNotEmpty()) append('\n'); append(value) } } }
            if (text.isBlank()) error("Gemini returned no readable text.")
            val sources = mutableListOf<AiSource>()
            val chunks = candidate.optJSONObject("groundingMetadata")?.optJSONArray("groundingChunks")
            if (chunks != null) for (i in 0 until chunks.length()) {
                val web = chunks.optJSONObject(i)?.optJSONObject("web") ?: continue
                val uri = web.optString("uri")
                if (uri.startsWith("https://") && sources.none { it.url == uri }) sources += AiSource(web.optString("title").ifBlank { "Retrieved source" }, uri)
            }
            AiReply(text, sources.take(12))
        } finally { connection.disconnect() }
    }
    companion object {
        fun userMessage(text: String) = AiMessage(UUID.randomUUID().toString(), "user", text)
        fun modelMessage(text: String) = AiMessage(UUID.randomUUID().toString(), "model", text)
    }
}
