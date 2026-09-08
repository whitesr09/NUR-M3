package com.nshd.nurm3.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Local backup codec for user-owned data. It deliberately uses a small stable JSON
 * shape so future versions can import older exports without depending on Room internals.
 */
object BackupCodec {
    const val CURRENT_VERSION = 1

    fun export(entries: List<Entry>, completions: List<Completion>, createdAt: Long = System.currentTimeMillis()): String {
        val root = JSONObject()
            .put("app", "NUR-M3")
            .put("schema", CURRENT_VERSION)
            .put("createdAt", createdAt)
        val entryArray = JSONArray()
        entries.forEach { entry ->
            entryArray.put(JSONObject()
                .put("id", entry.id)
                .put("kind", entry.kind)
                .put("title", entry.title)
                .put("position", entry.position)
                .put("createdAt", entry.createdAt)
                .put("archived", entry.archived)
                .put("schedule", entry.schedule)
                .put("weekdaysMask", entry.weekdaysMask)
                .put("startDate", entry.startDate)
                .put("endDate", entry.endDate))
        }
        val completionArray = JSONArray()
        completions.forEach { completion ->
            completionArray.put(JSONObject()
                .put("entryId", completion.entryId)
                .put("localDate", completion.localDate)
                .put("completedAt", completion.completedAt)
                .put("titleSnapshot", completion.titleSnapshot)
                .put("kindSnapshot", completion.kindSnapshot))
        }
        return root.put("entries", entryArray).put("completions", completionArray).toString(2)
    }

    fun inspect(rawJson: String): BackupSummary {
        val root = JSONObject(rawJson)
        require(root.optString("app") == "NUR-M3") { "Backup is not for NUR-M3" }
        val schema = root.optInt("schema", 0)
        require(schema in 1..CURRENT_VERSION) { "Unsupported backup schema: $schema" }
        return BackupSummary(
            schema = schema,
            createdAt = root.optLong("createdAt", 0L),
            entryCount = root.optJSONArray("entries")?.length() ?: 0,
            completionCount = root.optJSONArray("completions")?.length() ?: 0
        )
    }
}

data class BackupSummary(
    val schema: Int,
    val createdAt: Long,
    val entryCount: Int,
    val completionCount: Int
)
