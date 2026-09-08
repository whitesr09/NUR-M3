package com.nshd.nurm3

import com.nshd.nurm3.data.*
import com.nshd.nurm3.ui.*
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class ProgressAndScheduleTest {
    private val day = LocalDate.of(2026, 9, 8)
    private val zone = ZoneOffset.UTC
    private fun entry(id: String, kind: String = NurKind.AMANAH) = Entry(id, kind, id, 0, 1L, startDate = "2026-09-01")

    @Test fun onlyPrayersFormTheDenominatorWithoutCustomTasks() {
        val prayers = (1..5).map { entry("p$it", NurKind.PRAYER) }
        assertEquals(0.4f, DailyProgress.summary(prayers, listOf(Completion("p1", day.toString(), 1L), Completion("p2", day.toString(), 1L)), day, zone).fraction, 0.001f)
    }

    @Test fun previousDayDoesNotCompleteToday() {
        val task = entry("t")
        val records = listOf(Completion("t", day.minusDays(1).toString(), 1L))
        assertEquals(0, DailyProgress.summary(listOf(task), records, day, zone).completed)
        assertTrue(EntrySchedule.isActive(task, day, zone))
    }

    @Test fun archivedAndFutureEntriesDoNotChangeTodayProgress() {
        val active = entry("a")
        val archived = entry("b").copy(archived = true)
        val future = entry("c").copy(startDate = "2026-09-09")
        val records = listOf(Completion("a", day.toString(), 1L), Completion("b", day.toString(), 1L), Completion("c", day.toString(), 1L))
        assertEquals(ProgressSummary(1, 1), DailyProgress.summary(listOf(active, archived, future), records, day, zone))
    }

    @Test fun weeklyAndOnceSchedulesRespectDates() {
        val weekly = entry("w").copy(schedule = "weekly", startDate = "2026-09-01")
        assertTrue(EntrySchedule.isActive(weekly, day, zone))
        assertFalse(EntrySchedule.isActive(weekly, day.plusDays(1), zone))
        val once = entry("o").copy(schedule = "once", startDate = day.toString())
        assertTrue(EntrySchedule.isActive(once, day, zone))
        assertFalse(EntrySchedule.isActive(once, day.plusDays(1), zone))
    }

    @Test fun weekdayMaskIsMondayFirst() {
        val weekdays = entry("w").copy(schedule = "weekdays", weekdaysMask = 31)
        assertTrue(EntrySchedule.isActive(weekdays, day, zone))
        assertFalse(EntrySchedule.isActive(weekdays, LocalDate.of(2026, 9, 12), zone))
    }
}
