package com.nshd.nurm3

import com.nshd.nurm3.data.*
import com.nshd.nurm3.ui.*
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class ReflectionIntegrationTest {
    private val day = LocalDate.of(2026, 9, 9)
    private fun entry(id: String, kind: String) = Entry(id, kind, id, 0, 1L, startDate = "2026-09-01")

    @Test fun lightExcludesRhythmButRetainsOtherProgress() {
        val entries = (1..5).map { entry("prayer-$it", NurKind.PRAYER) } + listOf(entry("a", NurKind.AMANAH), entry("m", NurKind.MUHASABA), entry("r", NurKind.RHYTHM))
        val done = listOf("prayer-1", "a", "r").map { Completion(it, day.toString(), 1L) }
        assertEquals(ProgressSummary(2, 7), DailyProgress.lightSummary(entries, done, day, ZoneOffset.UTC))
        assertEquals(ProgressSummary(3, 8), DailyProgress.summary(entries, done, day, ZoneOffset.UTC))
    }

    @Test fun yesterdayAndInactiveEntriesNeverInflateLight() {
        val prayers = (1..5).map { entry("prayer-$it", NurKind.PRAYER) }
        val future = entry("future", NurKind.AMANAH).copy(startDate = day.plusDays(1).toString())
        val archived = entry("archived", NurKind.MUHASABA).copy(archived = true)
        val records = listOf(Completion("prayer-1", day.minusDays(1).toString(), 1L), Completion("future", day.toString(), 1L), Completion("archived", day.toString(), 1L))
        assertEquals(ProgressSummary(0, 5), DailyProgress.lightSummary(prayers + future + archived, records, day, ZoneOffset.UTC))
    }

    @Test fun reflectionSelectionWrapsAndDoesNotChangeTheDate() {
        val first = ReflectionLibrary.daily(day)
        assertEquals(first, ReflectionLibrary.at(day, 0))
        assertEquals(first, ReflectionLibrary.at(day, ReflectionLibrary.items.size))
        assertEquals(first, ReflectionLibrary.at(day, -ReflectionLibrary.items.size))
        assertEquals(ReflectionLibrary.items.size, (0 until ReflectionLibrary.items.size).map { ReflectionLibrary.at(day, it).id }.toSet().size)
        assertEquals(first, ReflectionLibrary.daily(day))
    }
}
