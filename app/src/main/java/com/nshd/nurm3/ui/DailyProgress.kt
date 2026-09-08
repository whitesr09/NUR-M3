package com.nshd.nurm3.ui

import com.nshd.nurm3.data.Completion
import com.nshd.nurm3.data.Entry
import java.time.LocalDate

/** Pure date-scoped progress calculation. Entries are never reset at midnight. */
object DailyProgress {
    fun completedIds(completions: List<Completion>, date: LocalDate): Set<String> =
        completions.asSequence().filter { it.localDate == date.toString() }.map { it.entryId }.toSet()

    fun fraction(entries: List<Entry>, completions: List<Completion>, date: LocalDate): Float {
        if (entries.isEmpty()) return 0f
        val done = completedIds(completions, date)
        return entries.count { it.id in done }.toFloat() / entries.size
    }
}
