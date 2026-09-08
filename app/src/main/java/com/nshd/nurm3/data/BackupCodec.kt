package com.nshd.nurm3.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Portable user-owned data; no database internals, credentials or device secrets. */
data class BackupPayload(
    val entries: List<Entry>,
    val completions: List<Completion>,
    val createdAt: Long,
    val settings: JSONObject? = null
)

data class BackupSummary(
    val schema: Int,
    val createdAt: Long,
    val entryCount: Int,
    val completionCount: Int
)

object BackupCodec {
    const val CURRENT_VERSION = 1
    const val MAX_BYTES = 5 * 1024 * 1024
    private val kinds = setOf(NurKind.PRAYER, NurKind.AMANAH, NurKind.MUHASABA, NurKind.RHYTHM)
    private val schedules = setOf("daily", "weekdays", "weekly", "once")

    fun export(entries: List<Entry>, completions: List<Completion>, createdAt: Long = System.currentTimeMillis(), settings: JSONObject? = null): String {
        validate(entries, completions)
        val root = JSONObject().put("app", "NUR-M3").put("schema", CURRENT_VERSION).put("createdAt", createdAt)
        val entryArray = JSONArray()
        entries.forEach { e ->
            entryArray.put(JSONObject().put("id", e.id).put("kind", e.kind).put("title", e.title)
                .put("position", e.position).put("createdAt", e.createdAt).put("archived", e.archived)
                .put("schedule", e.schedule).put("weekdaysMask", e.weekdaysMask)
                .put("startDate", e.startDate).put("endDate", e.endDate))
        }
        val completionArray = JSONArray()
        completions.forEach { c ->
            completionArray.put(JSONObject().put("entryId", c.entryId).put("localDate", c.localDate)
                .put("completedAt", c.completedAt).put("titleSnapshot", c.titleSnapshot)
                .put("kindSnapshot", c.kindSnapshot))
        }
        root.put("entries", entryArray).put("completions", completionArray)
        if (settings != null) root.put("settings", settings)
        return root.toString(2)
    }

    fun inspect(rawJson: String): BackupSummary {
        val p = decode(rawJson)
        return BackupSummary(CURRENT_VERSION, p.createdAt, p.entries.size, p.completions.size)
    }

    /** Decode everything before any database mutation. Invalid/unknown data is rejected. */
    fun decode(rawJson: String): BackupPayload {
        require(rawJson.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup exceeds the 5 MB safety limit" }
        val root = JSONObject(rawJson)
        require(root.optString("app") == "NUR-M3") { "This backup is not for NUR-M3" }
        val schema = root.getInt("schema")
        require(schema == CURRENT_VERSION) { "Unsupported backup schema: $schema" }
        val entryArray = root.getJSONArray("entries")
        val completionArray = root.getJSONArray("completions")
        require(entryArray.length() <= 10000 && completionArray.length() <= 100000) { "Backup contains too many records" }
        val entries = (0 until entryArray.length()).map { i ->
            val e = entryArray.getJSONObject(i)
            Entry(
                id = e.getString("id"), kind = e.getString("kind"), title = e.getString("title"),
                position = e.getInt("position"), createdAt = e.getLong("createdAt"),
                archived = e.optBoolean("archived", false), schedule = e.optString("schedule", "daily"),
                weekdaysMask = e.optInt("weekdaysMask", 127), startDate = e.optNullableString("startDate"),
                endDate = e.optNullableString("endDate")
            )
        }
        val completions = (0 until completionArray.length()).map { i ->
            val c = completionArray.getJSONObject(i)
            Completion(c.getString("entryId"), c.getString("localDate"), c.getLong("completedAt"),
                c.optString("titleSnapshot", ""), c.optString("kindSnapshot", ""))
        }
        validate(entries, completions)
        return BackupPayload(entries, completions, root.optLong("createdAt", 0L), root.optJSONObject("settings"))
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (isNull(key)) null else getString(key)

    fun validate(entries: List<Entry>, completions: List<Completion>) {
        require(entries.size <= 10000 && completions.size <= 100000) { "Too many records" }
        require(entries.map { it.id }.toSet().size == entries.size) { "Duplicate entry IDs" }
        val byId = entries.associateBy { it.id }
        entries.forEach { e ->
            require(e.id.isNotBlank() && e.id.length <= 128 && e.kind in kinds) { "Invalid entry identity" }
            require(e.title.isNotBlank() && e.title.length <= 200 && e.position >= 0 && e.createdAt >= 0) { "Invalid entry" }
            require(e.schedule in schedules && e.weekdaysMask in 1..127) { "Invalid recurrence" }
            val start = e.startDate?.let(LocalDate::parse)
            val end = e.endDate?.let(LocalDate::parse)
            require(start == null || end == null || !end.isBefore(start)) { "Invalid date range" }
        }
        require(completions.map { it.entryId to it.localDate }.toSet().size == completions.size) { "Duplicate completion records" }
        completions.forEach { c ->
            val owner = byId[c.entryId] ?: throw IllegalArgumentException("Completion refers to a missing entry")
            require(c.localDate == LocalDate.parse(c.localDate).toString() && c.completedAt >= 0) { "Invalid completion date" }
            require(c.titleSnapshot.length <= 200 && (c.kindSnapshot.isBlank() || c.kindSnapshot == owner.kind)) { "Invalid completion snapshot" }
        }
    }
}
