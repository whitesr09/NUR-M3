package com.nshd.nurm3.ui

import com.nshd.nurm3.data.*
import java.time.LocalDate
import java.time.ZoneId

/** Historical counts are actual records, not reconstructed claims about missed tasks. */
data class RecordedDay(val date: LocalDate, val total: Int, val byKind: Map<String, Int>)
data class ActivityReport(
    val days: List<RecordedDay>,
    val recordedCompletions: Int,
    val recordedDays: Int,
    val byKind: Map<String, Int>,
    val firstRecordedDate: LocalDate?
)

object ActivityInsights {
    fun report(completions: List<Completion>, today: LocalDate, days: Int): ActivityReport {
        require(days in 1..366)
        val first = today.minusDays(days.toLong() - 1)
        val records = completions.filter { c ->
            val date = runCatching { LocalDate.parse(c.localDate) }.getOrNull()
            date != null && !date.isBefore(first) && !date.isAfter(today)
        }
        val grouped = records.groupBy { LocalDate.parse(it.localDate) }
        val series = (0 until days).map { offset ->
            val date = first.plusDays(offset.toLong())
            val items = grouped[date].orEmpty()
            RecordedDay(date, items.size, items.groupingBy { it.kindSnapshot }.eachCount())
        }
        return ActivityReport(series, records.size, grouped.size, records.groupingBy { it.kindSnapshot }.eachCount(), grouped.keys.minOrNull())
    }

    /** A schedule-based streak. Old schedule edits are not treated as historical evidence. */
    fun currentStreak(entry: Entry, completions: List<Completion>, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Int {
        if (entry.kind == NurKind.PRAYER || entry.schedule == "once" || entry.archived) return 0
        val done = completions.asSequence().filter { it.entryId == entry.id }.mapNotNull { runCatching { LocalDate.parse(it.localDate) }.getOrNull() }.toSet()
        val active = entry.copy(archived = false)
        var date = today
        var streak = 0
        var skippedToday = false
        repeat(3660) {
            if (EntrySchedule.isActive(active, date, zone)) {
                if (date in done) streak++
                else if (date == today && !skippedToday) skippedToday = true
                else return streak
            }
            date = date.minusDays(1)
        }
        return streak
    }
}
