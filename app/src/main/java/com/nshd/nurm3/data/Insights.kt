package com.nshd.nurm3.data

import com.nshd.nurm3.ui.DailyProgress
import com.nshd.nurm3.ui.ProgressSummary
import java.time.LocalDate
import java.time.ZoneId

/** Insights use recorded data only; no missing days are invented. */
data class DayInsight(val date: LocalDate, val summary: ProgressSummary)
data class HabitInsight(val completedDays: Int, val currentStreak: Int, val longestStreak: Int)

object NurInsights {
    fun daily(entries: List<Entry>, completions: List<Completion>, start: LocalDate, end: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<DayInsight> {
        require(!end.isBefore(start))
        require(end.toEpochDay() - start.toEpochDay() <= 3660) { "Date range too large" }
        return generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }
            .map { DayInsight(it, DailyProgress.summary(entries, completions, it, zone)) }.toList()
    }

    fun habit(entry: Entry, completions: List<Completion>, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): HabitInsight {
        val dates = completions.asSequence().filter { it.entryId == entry.id }
            .mapNotNull { runCatching { LocalDate.parse(it.localDate) }.getOrNull() }
            .filter { !it.isAfter(today) }.toSet()
        val first = entry.startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: today
        val start = first.coerceAtMost(today)
        var longest = 0
        var run = 0
        var current = 0
        var date = start
        while (!date.isAfter(today)) {
            if (EntrySchedule.isActive(entry, date, zone)) {
                if (date in dates) {
                    run++
                    longest = maxOf(longest, run)
                } else run = 0
            }
            date = date.plusDays(1)
        }
        date = today
        while (!date.isBefore(start)) {
            if (EntrySchedule.isActive(entry, date, zone)) {
                if (date in dates) current++
                else if (date == today) { /* An unfinished current day does not break yesterday's streak. */ }
                else break
            }
            date = date.minusDays(1)
        }
        return HabitInsight(dates.size, current, longest)
    }
}
