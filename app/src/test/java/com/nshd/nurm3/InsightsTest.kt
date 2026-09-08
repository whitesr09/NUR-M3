package com.nshd.nurm3

import com.nshd.nurm3.data.*
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class InsightsTest {
    private val today = LocalDate.of(2026, 9, 8)
    private val entry = Entry("habit", NurKind.RHYTHM, "Read", 0, 1L, startDate = "2026-09-01")
    private fun record(date: String) = Completion("habit", date, 1L)

    @Test fun yesterdayDoesNotCountAsToday() {
        val days = NurInsights.daily(listOf(entry), listOf(record("2026-09-07")), today.minusDays(1), today, ZoneOffset.UTC)
        assertEquals(1, days[0].summary.completed)
        assertEquals(0, days[1].summary.completed)
    }

    @Test fun streakAllowsUnfinishedToday() {
        val records = listOf(record("2026-09-06"), record("2026-09-07"))
        assertEquals(2, NurInsights.habit(entry, records, today, ZoneOffset.UTC).currentStreak)
    }

    @Test fun scheduledDaysAreUsedForStreak() {
        val weekly = entry.copy(schedule = "weekly")
        val records = listOf(record("2026-09-01"), record("2026-09-08"))
        val result = NurInsights.habit(weekly, records, today, ZoneOffset.UTC)
        assertEquals(2, result.currentStreak)
        assertEquals(2, result.longestStreak)
    }

    @Test fun futureRecordsDoNotEnterInsights() {
        assertEquals(0, NurInsights.habit(entry, listOf(record("2026-09-09")), today, ZoneOffset.UTC).completedDays)
    }
}
