package com.nshd.nurm3

import com.nshd.nurm3.data.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class DhikrHistoryTest {
    private val today = LocalDate.of(2026, 9, 9)
    private val days = listOf(
        DhikrDay("a", "2026-09-08", 20),
        DhikrDay("b", "2026-09-08", 9),
        DhikrDay("a", "2026-09-09", 13),
        DhikrDay("a", "2026-08-10", 4),
        DhikrDay("a", "2026-08-09", 3),
        DhikrDay("a", "2026-09-10", 2),
        DhikrDay("a", "2026-09-07", 0)
    )

    @Test fun onlyRecordedPositiveCountsBelongingToThePhraseAreShown() {
        val rows = DhikrHistory.rows(days, "a")
        assertEquals(5, rows.size)
        assertEquals(LocalDate.of(2026, 9, 10), rows.first().date)
        assertEquals(2L, rows.first().count)
        assertEquals(42L, DhikrHistory.summary(rows).total)
        assertEquals(5, DhikrHistory.summary(rows).recordedDays)
        assertTrue(DhikrHistory.rows(days, "missing").isEmpty())
    }

    @Test fun recentWindowIncludesBothBoundaryDatesAndExcludesFutureDates() {
        val boundary = DhikrDay("a", "2026-08-11", 4)
        val rows = DhikrHistory.recent(DhikrHistory.rows(days + boundary, "a"), today)
        assertEquals(listOf("2026-09-09", "2026-09-08", "2026-08-11"), rows.map { it.date.toString() })
        assertEquals(37L, DhikrHistory.summary(rows).total)
        assertThrows(IllegalArgumentException::class.java) { DhikrHistory.recent(emptyList(), today, 0) }
    }

    @Test fun sessionResetAndArchiveDoNotModifyHistory() {
        val phrase = DhikrPhrase("a", "Personal phrase", 33, 33, 0, 1L)
        val original = DhikrHistory.rows(days, phrase.id)
        assertEquals(original, DhikrHistory.rows(days, phrase.copy(sessionCount = 0).id))
        assertEquals(original, DhikrHistory.rows(days, phrase.copy(archived = true).id))
    }

    @Test fun invalidDatesAreNotInventedAndEmptyHistoryHasNoLastDate() {
        val rows = DhikrHistory.rows(listOf(DhikrDay("a", "not-a-date", 3)), "a")
        assertTrue(rows.isEmpty())
        assertEquals(DhikrHistorySummary(0, 0, null), DhikrHistory.summary(rows))
    }
}
