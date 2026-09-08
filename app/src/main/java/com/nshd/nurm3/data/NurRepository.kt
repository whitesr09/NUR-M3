package com.nshd.nurm3.data

import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow

object NurKind {
    const val PRAYER = "prayer"
    const val AMANAH = "amanah"
    const val MUHASABA = "muhasaba"
}

class NurRepository(private val dao: NurDao) {
    val entries: Flow<List<Entry>> = dao.observeEntries()
    val completions: Flow<List<Completion>> = dao.observeCompletions()

    suspend fun add(kind: String, title: String, position: Int = 0) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        dao.saveEntry(Entry(UUID.randomUUID().toString(), kind, clean, position, System.currentTimeMillis()))
    }

    suspend fun setCompleted(id: String, date: LocalDate, completed: Boolean) {
        if (completed) dao.saveCompletion(Completion(id, date.toString(), System.currentTimeMillis()))
        else dao.removeCompletion(id, date.toString())
    }

    suspend fun delete(id: String) = dao.deleteEntry(id)
}
