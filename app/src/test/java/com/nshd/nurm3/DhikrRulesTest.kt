package com.nshd.nurm3

import com.nshd.nurm3.data.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DhikrRulesTest {
    private val phrase = DhikrPhrase("custom-a", "My remembrance", 33, 0, 0, 1L)

    @Test fun validatesPersonalTargetsAndBoundedCounters() {
        assertTrue(DhikrRules.valid(phrase))
        assertTrue(DhikrRules.valid(phrase.copy(target = 1)))
        assertTrue(DhikrRules.valid(phrase.copy(target = DhikrRules.MAX_TARGET)))
        assertFalse(DhikrRules.valid(phrase.copy(target = 0)))
        assertFalse(DhikrRules.valid(phrase.copy(target = DhikrRules.MAX_TARGET + 1)))
        assertFalse(DhikrRules.valid(phrase.copy(title = " ")))
        assertFalse(DhikrRules.valid(phrase.copy(sessionCount = -1)))
    }

    @Test fun incrementsAreBoundedAndDoNotStopAtThePersonalGoal() {
        assertEquals(1L to 1L, DhikrRules.next(0, 0))
        assertEquals(34L to 8L, DhikrRules.next(33, 7))
        assertFalse(DhikrRules.canIncrement(DhikrRules.MAX_COUNT, 0))
        assertFalse(DhikrRules.canIncrement(0, DhikrRules.MAX_COUNT))
        assertTrue(DhikrRules.canIncrement(DhikrRules.MAX_COUNT - 1, 0))
        assertThrows(IllegalArgumentException::class.java) { DhikrRules.next(DhikrRules.MAX_COUNT, 0) }
    }

    @Test fun progressIsFiniteAndClamped() {
        assertEquals(0f, DhikrRules.progress(0, 33), 0.0001f)
        assertEquals(0f, DhikrRules.progress(-10, 33), 0.0001f)
        assertEquals(1f, DhikrRules.progress(33, 33), 0.0001f)
        assertEquals(1f, DhikrRules.progress(100, 33), 0.0001f)
        assertEquals(0f, DhikrRules.progress(10, 0), 0.0001f)
    }

    @Test fun sessionResetAndArchiveLeaveDatedHistoryUntouched() {
        val yesterday = LocalDate.of(2026, 9, 8)
        val today = yesterday.plusDays(1)
        val days = listOf(DhikrDay(phrase.id, yesterday.toString(), 20), DhikrDay(phrase.id, today.toString(), 13))
        val completed = phrase.copy(sessionCount = 33)
        val reset = completed.copy(sessionCount = 0)
        val archived = reset.copy(archived = true)
        assertEquals(33L, DhikrRules.total(days))
        assertEquals(13L, days.filter { it.localDate == today.toString() }.sumOf { it.count })
        assertEquals(0L, reset.sessionCount)
        assertTrue(archived.archived)
        assertEquals(33L, DhikrRules.total(days))
    }

    @Test fun differentPhrasesHaveIndependentTotals() {
        val days = listOf(DhikrDay("a", "2026-09-08", 4), DhikrDay("b", "2026-09-08", 9), DhikrDay("a", "2026-09-09", 3))
        assertEquals(7L, DhikrRules.total(days.filter { it.phraseId == "a" }))
        assertEquals(9L, DhikrRules.total(days.filter { it.phraseId == "b" }))
    }
}
