package com.nshd.nurm3

import com.nshd.nurm3.data.*
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class InsightsBoundaryTest {
    private val date = LocalDate.of(2026, 9, 8)
    private val task = Entry("t", NurKind.RHYTHM, "Habit", 0, 1L, startDate = "2026-09-01")
    private fun done(day: String) = Completion("t", day, 1L)

    @Test fun futureAndUnscheduledRecordsAreIgnored() {
        val weekly = task.copy(schedule = "weekly")
        val result = NurInsights.habit(weekly, listOf(done("2026-09-01"), done("2026-09-02"), done("2026-09-08"), done("2026-09-09")), date, ZoneOffset.UTC)
        assertEquals(2, result.completedDays)
        assertEquals(2, result.currentStreak)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOversizedRange() {
        NurInsights.daily(emptyList(), emptyList(), date.minusDays(4000), date)
    }
}
