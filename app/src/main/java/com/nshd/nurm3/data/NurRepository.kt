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

    suspend fun saveNew(entry: Entry) {
        if (!valid(entry)) return
        dao.saveEntry(entry.copy(id = UUID.randomUUID().toString(), title = entry.title.trim(), archived = false, createdAt = System.currentTimeMillis()))
    }

    suspend fun setCompleted(id: String, date: LocalDate, completed: Boolean) {
        val entry = dao.getEntry(id) ?: return
        if (!EntrySchedule.isActive(entry, date)) return
        dao.recordCompletion(id, date.toString(), completed, System.currentTimeMillis())
    }

    suspend fun updateEntry(entry: Entry) {
        if (!valid(entry)) return
        val existing = dao.getEntry(entry.id) ?: return
        if (existing.kind != entry.kind || existing.archived) return
        dao.saveEntry(entry.copy(title = entry.title.trim(), position = existing.position, createdAt = existing.createdAt, archived = existing.archived))
    }

    suspend fun delete(id: String) = dao.deleteEntry(id)
}
