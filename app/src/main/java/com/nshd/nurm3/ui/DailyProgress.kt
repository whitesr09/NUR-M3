package com.nshd.nurm3.ui

import com.nshd.nurm3.data.*
import java.time.LocalDate
import java.time.ZoneId

data class ProgressSummary(val completed: Int, val total: Int) {
    val fraction: Float get() = if (total == 0) 0f else completed.toFloat() / total
}

/** A single date-scoped calculation shared by every screen and future widget. */
object DailyProgress {
    fun completedIds(completions: List<Completion>, date: LocalDate): Set<String> =
        completions.asSequence().filter { it.localDate == date.toString() }.map { it.entryId }.toSet()

    fun summary(entries: List<Entry>, completions: List<Completion>, date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): ProgressSummary {
        val active = entries.filter { EntrySchedule.isActive(it, date, zone) }
        val done = completedIds(completions, date)
        return ProgressSummary(active.count { it.id in done }, active.size)
    }

    fun fraction(entries: List<Entry>, completions: List<Completion>, date: LocalDate): Float =
        summary(entries, completions, date).fraction
}
