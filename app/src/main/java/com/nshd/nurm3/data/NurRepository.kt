package com.nshd.nurm3.data

import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow

object NurKind {
    const val PRAYER = "prayer"
    const val AMANAH = "amanah"
    const val MUHASABA = "muhasaba"
    const val RHYTHM = "rhythm"
}

class NurRepository(private val dao: NurDao) {
    val entries: Flow<List<Entry>> = dao.observeEntries()
    val allEntries: Flow<List<Entry>> = dao.observeAllEntries()
    val completions: Flow<List<Completion>> = dao.observeCompletions()

    private fun valid(entry: Entry): Boolean =
        entry.kind in setOf(NurKind.AMANAH, NurKind.MUHASABA, NurKind.RHYTHM) &&
            entry.title.isNotBlank() && entry.title.length <= 200 &&
            entry.schedule in setOf("daily", "weekdays", "weekly", "once") &&
            (entry.schedule != "weekdays" || entry.weekdaysMask in 1..127) &&
            runCatching {
                val start = LocalDate.parse(entry.startDate ?: return@runCatching false)
                entry.endDate == null || !LocalDate.parse(entry.endDate).isBefore(start)
            }.getOrDefault(false)

    suspend fun add(kind: String, title: String, position: Int = 0, date: LocalDate = LocalDate.now()) {
        saveNew(Entry(UUID.randomUUID().toString(), kind, title.trim(), position, System.currentTimeMillis(), startDate = date.toString()))
    }

    suspend fun saveNew(entry: Entry): Boolean {
        if (!valid(entry)) return false
        dao.saveEntry(entry.copy(id = UUID.randomUUID().toString(), title = entry.title.trim(), archived = false, createdAt = System.currentTimeMillis()))
        return true
    }

    suspend fun setCompleted(id: String, date: LocalDate, completed: Boolean): Boolean {
        val entry = dao.getEntry(id) ?: return false
        if (!EntrySchedule.isActive(entry, date)) return false
        dao.recordCompletion(id, date.toString(), completed, System.currentTimeMillis())
        return true
    }

    suspend fun updateEntry(entry: Entry): Boolean {
        if (!valid(entry)) return false
        val existing = dao.getEntry(entry.id) ?: return false
        if (existing.kind != entry.kind || existing.archived) return false
        dao.saveEntry(entry.copy(title = entry.title.trim(), position = existing.position, createdAt = existing.createdAt, archived = existing.archived))
        return true
    }

    /** Archive keeps every dated completion. Prayer definitions cannot be archived. */
    suspend fun delete(id: String): Boolean {
        val entry = dao.getEntry(id) ?: return false
        if (entry.kind == NurKind.PRAYER || entry.archived) return false
        dao.deleteEntry(id)
        return true
    }

    suspend fun restore(id: String): Boolean {
        val entry = dao.getEntry(id) ?: return false
        if (entry.kind == NurKind.PRAYER || !entry.archived) return false
        dao.saveEntry(entry.copy(archived = false))
        return true
    }
}
