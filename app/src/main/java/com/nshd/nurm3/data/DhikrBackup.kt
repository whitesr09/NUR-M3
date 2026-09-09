package com.nshd.nurm3.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Versioned portable counter data. No credentials, device identifiers or private keys. */
object DhikrBackup {
    private const val MAX_PHRASES = 10_000
    private const val MAX_DAYS = 200_000

    fun encodePhrases(phrases: List<DhikrPhrase>): JSONArray = JSONArray().also { array ->
        phrases.forEach { p -> array.put(JSONObject()
            .put("id", p.id).put("title", p.title).put("target", p.target)
            .put("sessionCount", p.sessionCount).put("position", p.position)
            .put("createdAt", p.createdAt).put("archived", p.archived)) }
    }

    fun encodeDays(days: List<DhikrDay>): JSONArray = JSONArray().also { array ->
        days.forEach { d -> array.put(JSONObject().put("phraseId", d.phraseId)
            .put("localDate", d.localDate).put("count", d.count)) }
    }

    fun decode(root: JSONObject): Pair<List<DhikrPhrase>, List<DhikrDay>> {
        val schema = root.getInt("schema")
        if (schema < 3) return emptyList<DhikrPhrase>() to emptyList()
        val definitions = root.getJSONArray("dhikrPhrases")
        val records = root.getJSONArray("dhikrDays")
        require(definitions.length() <= MAX_PHRASES && records.length() <= MAX_DAYS) { "Too many Dhikr records" }
        val phrases = (0 until definitions.length()).map { i ->
            val p = definitions.getJSONObject(i)
            DhikrPhrase(p.getString("id"), p.getString("title"), p.getInt("target"),
                p.getLong("sessionCount"), p.getInt("position"), p.getLong("createdAt"), p.getBoolean("archived"))
        }
        val days = (0 until records.length()).map { i ->
            val d = records.getJSONObject(i)
            DhikrDay(d.getString("phraseId"), LocalDate.parse(d.getString("localDate")).toString(), d.getLong("count"))
        }
        validate(phrases, days)
        return phrases to days
    }

    fun validate(phrases: List<DhikrPhrase>, days: List<DhikrDay>) {
        require(phrases.size <= MAX_PHRASES && days.size <= MAX_DAYS) { "Too many Dhikr records" }
        require(phrases.all { DhikrRules.valid(it) && it.position >= 0 }) { "Invalid Dhikr phrase" }
        require(phrases.map { it.id }.distinct().size == phrases.size) { "Duplicate Dhikr phrase" }
        val ids = phrases.map { it.id }.toSet()
        require(days.all {
            it.phraseId in ids && it.count in 1..DhikrRules.MAX_COUNT &&
                runCatching { LocalDate.parse(it.localDate).toString() == it.localDate }.getOrDefault(false)
        }) { "Invalid Dhikr history" }
        require(days.map { it.phraseId to it.localDate }.distinct().size == days.size) { "Duplicate Dhikr day" }
        days.groupBy { it.phraseId }.values.forEach { DhikrRules.total(it) }
    }
}
