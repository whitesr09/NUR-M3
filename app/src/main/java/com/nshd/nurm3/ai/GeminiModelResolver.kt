package com.nshd.nurm3.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Resolves a real generateContent-capable Gemini model for the user's API key. */
internal object GeminiModelResolver {
    private data class Cache(val keyHash: Int, val at: Long, val models: List<String>)
    @Volatile private var cache: Cache? = null
    private const val CACHE_MS = 10 * 60 * 1000L

    private val preferredOrder = listOf(
        "gemini-3.8-flash",
        "gemini-3.7-flash",
        "gemini-3.6-flash",
        "gemini-3.5-flash",
        "gemini-3.5-flash-lite",
        "gemini-3.1-flash-lite",
        "gemini-flash-latest"
    )

    suspend fun resolve(key: String, preferred: String): String = withContext(Dispatchers.IO) {
        val models = availableModels(key)
        when {
            preferred in models -> preferred
            else -> preferredOrder.firstOrNull { it in models }
                ?: models.firstOrNull { it.contains("flash", ignoreCase = true) }
                ?: models.firstOrNull()
                ?: error("No Gemini text model with generateContent access is available for this API key.")
        }
    }

    suspend fun availableModels(key: String, force: Boolean = false): List<String> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val keyHash = key.hashCode()
        val existing = cache
        if (!force && existing != null && existing.keyHash == keyHash && now - existing.at < CACHE_MS) {
            return@withContext existing.models
        }

        val connection = (URL("https://generativelanguage.googleapis.com/v1beta/models?pageSize=100").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("x-goog-api-key", key)
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                val message = when (code) {
                    400 -> "Gemini rejected the API key or model-list request."
                    401, 403 -> "Gemini authentication failed. Create a Gemini API key in Google AI Studio and check project access."
                    429 -> "Gemini quota is currently exhausted. Try again later or review the API project's quota."
                    else -> "Could not load Gemini models (HTTP $code)."
                }
                error(message)
            }
            val text = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            require(text.length <= 1_048_576) { "Gemini model list was unexpectedly large." }
            val array = JSONObject(text).optJSONArray("models")
                ?: error("Gemini returned no model list.")
            val models = buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val methods = item.optJSONArray("supportedGenerationMethods") ?: continue
                    var supportsGenerate = false
                    for (j in 0 until methods.length()) {
                        if (methods.optString(j) == "generateContent") {
                            supportsGenerate = true
                            break
                        }
                    }
                    if (!supportsGenerate) continue
                    val rawName = item.optString("name")
                    val id = rawName.removePrefix("models/")
                    if (id.startsWith("gemini-") && id.length in 3..100) add(id)
                }
            }.distinct()
            if (models.isEmpty()) error("This API key has no Gemini text models available for generateContent.")
            cache = Cache(keyHash, now, models)
            models
        } finally {
            connection.disconnect()
        }
    }
}
