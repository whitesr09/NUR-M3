package com.nshd.nurm3.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Portable, versioned and deliberately free of credentials and PIN material. */
data class BackupPayload(
    val entries: List<Entry>, val completions: List<Completion>, val createdAt: Long,
    val dhikrPhrases: List<DhikrPhrase> = emptyList(), val dhikrDays: List<DhikrDay> = emptyList(),
    val schema: Int = BackupCodec.CURRENT_VERSION
)
data class BackupSummary(
    val schema: Int, val createdAt: Long, val entryCount: Int, val completionCount: Int,
    val dhikrPhraseCount: Int = 0, val dhikrDayCount: Int = 0
)

object BackupCodec {
    const val CURRENT_VERSION = 3
    const val MAX_BYTES = 8 * 1024 * 1024
    private val kinds = setOf(NurKind.PRAYER, NurKind.AMANAH, NurKind.MUHASABA, NurKind.RHYTHM)
    private val schedules = setOf("daily", "weekdays", "weekly", "once")
    private fun String.date(): String = LocalDate.parse(this).toString()
    private fun JSONObject.optionalDate(key: String): String? =
        if (!has(key) || isNull(key)) null else getString(key).takeUnless { it.isBlank() || it == "null" }?.date()

    fun export(
        entries: List<Entry>, completions: List<Completion>, createdAt: Long = System.currentTimeMillis(),
        dhikrPhrases: List<DhikrPhrase> = emptyList(), dhikrDays: List<DhikrDay> = emptyList()
    ): String {
        val root = JSONObject().put("app", "NUR-M3").put("schema", CURRENT_VERSION).put("createdAt", createdAt)
        val definitions = JSONArray()
        entries.forEach { e -> definitions.put(JSONObject().put("id", e.id).put("kind", e.kind).put("title", e.title).put("position", e.position).put("createdAt", e.createdAt).put("archived", e.archived).put("schedule", e.schedule).put("weekdaysMask", e.weekdaysMask).put("startDate", e.startDate ?: JSONObject.NULL).put("endDate", e.endDate ?: JSONObject.NULL)) }
        val records = JSONArray()
        completions.forEach { c -> records.put(JSONObject().put("entryId", c.entryId).put("localDate", c.localDate).put("completedAt", c.completedAt).put("titleSnapshot", c.titleSnapshot).put("kindSnapshot", c.kindSnapshot)) }
        return root.put("entries", definitions).put("completions", records)
            .put("dhikrPhrases", DhikrBackup.encodePhrases(dhikrPhrases))
            .put("dhikrDays", DhikrBackup.encodeDays(dhikrDays)).toString(2)
    }

    fun inspect(rawJson: String): BackupSummary {
        val root = root(rawJson)
        return BackupSummary(root.getInt("schema"), root.optLong("createdAt"), root.getJSONArray("entries").length(), root.getJSONArray("completions").length(), root.optJSONArray("dhikrPhrases")?.length() ?: 0, root.optJSONArray("dhikrDays")?.length() ?: 0)
    }

    private fun root(raw: String): JSONObject {
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup exceeds the 8 MB safety limit" }
        val root = JSONObject(raw)
        require(root.optString("app") == "NUR-M3") { "This is not a NUR-M3 backup" }
        require(root.optInt("schema") in 1..CURRENT_VERSION) { "Unsupported backup version" }
        require(root.has("entries") && root.has("completions")) { "Backup is incomplete" }
        require(root.getJSONArray("entries").length() <= 20000 && root.getJSONArray("completions").length() <= 200000) { "Backup contains too many records" }
        if (root.getInt("schema") >= 3) {
            require(root.has("dhikrPhrases") && root.has("dhikrDays")) { "Dhikr backup is incomplete" }
            root.getJSONArray("dhikrPhrases")
            root.getJSONArray("dhikrDays")
        }
        return root
    }

    fun decode(raw: String): BackupPayload {
        val root = root(raw)
        val entries = root.getJSONArray("entries")
        val definitions = (0 until entries.length()).map { i ->
            val e = entries.getJSONObject(i)
            Entry(e.getString("id"), e.getString("kind"), e.getString("title"), e.getInt("position"), e.getLong("createdAt"), e.optBoolean("archived"), e.optString("schedule", "daily"), e.optInt("weekdaysMask", 127), e.optionalDate("startDate"), e.optionalDate("endDate"))
        }
        require(definitions.map { it.id }.distinct().size == definitions.size) { "Duplicate entry identifiers" }
        definitions.forEach { e ->
            require(e.id.isNotBlank() && e.id.length <= 128 && e.kind in kinds && e.title.isNotBlank() && e.title.length <= 200) { "Invalid entry" }
            require(e.schedule in schedules && e.weekdaysMask in 0..127) { "Invalid recurrence" }
            require(e.endDate == null || e.startDate == null || !LocalDate.parse(e.endDate).isBefore(LocalDate.parse(e.startDate))) { "Invalid date range" }
            if (e.kind == NurKind.PRAYER) require(e.id in (1..5).map { "prayer-$it" } && !e.archived) { "Invalid prayer definition" }
        }
        val records = root.getJSONArray("completions")
        val ids = definitions.map { it.id }.toSet()
        val completions = (0 until records.length()).map { i ->
            val c = records.getJSONObject(i)
            Completion(c.getString("entryId"), c.getString("localDate").date(), c.getLong("completedAt"), c.optString("titleSnapshot"), c.optString("kindSnapshot"))
        }
        require(completions.all { it.entryId in ids || it.entryId in (1..5).map { n -> "prayer-$n" } }) { "A completion refers to a missing entry" }
        require(completions.map { it.entryId to it.localDate }.distinct().size == completions.size) { "Duplicate completion records" }
        require(completions.all { it.titleSnapshot.length <= 200 && (it.kindSnapshot.isBlank() || it.kindSnapshot in kinds) }) { "Invalid completion snapshot" }
        val (phrases, days) = DhikrBackup.decode(root)
        return BackupPayload(definitions, completions, root.optLong("createdAt"), phrases, days, root.getInt("schema"))
    }

    /** Recheck a payload before any database mutation, even if supplied by another caller. */
    fun validate(payload: BackupPayload) {
        require(payload.schema in 1..CURRENT_VERSION)
        require(payload.schema >= 3 || (payload.dhikrPhrases.isEmpty() && payload.dhikrDays.isEmpty()))
        decode(export(payload.entries, payload.completions, payload.createdAt, payload.dhikrPhrases, payload.dhikrDays))
    }
}
