package com.nshd.nurm3.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** All database import changes are committed together or rolled back together. */
class BackupService(private val context: Context) {
    private val database get() = NurDatabase.get(context)
    private val dao get() = database.dao()

    suspend fun export(): String = withContext(Dispatchers.IO) {
        val snapshot = database.withTransaction {
            dao.snapshotEntries() to dao.snapshotCompletions()
        }
        BackupCodec.export(snapshot.first, snapshot.second)
    }

    suspend fun import(raw: String, replace: Boolean): ImportResult = withContext(Dispatchers.IO) {
        val payload = BackupCodec.decode(raw)
        database.withTransaction {
            if (replace) {
                dao.clearCompletionsForRestore()
                dao.clearEntriesForRestore()
            }
            val importedEntries = dao.insertEntriesIfAbsent(payload.entries).count { it != -1L }
            // Merge never overwrites a user's existing definition or completion.
            val available = dao.snapshotEntries().associateBy { it.id }
            val accepted = payload.completions.filter { c ->
                available[c.entryId]?.kind == payload.entries.firstOrNull { it.id == c.entryId }?.kind
            }
            val importedCompletions = dao.insertCompletionsIfAbsent(accepted).count { it != -1L }
            // Keep the five canonical prayer definitions available after any restore.
            val names = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
            names.forEachIndexed { index, name ->
                val id = "prayer-${index + 1}"
                if (dao.getEntry(id) == null) dao.saveEntry(
                    Entry(id, NurKind.PRAYER, name, index, System.currentTimeMillis())
                )
            }
            ImportResult(importedEntries, importedCompletions, replace)
        }
    }
}

data class ImportResult(val entriesAdded: Int, val completionsAdded: Int, val replaced: Boolean)
