package com.nshd.nurm3

import com.nshd.nurm3.data.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class DailyCompletionTest {
    @Test fun completionIsScopedToItsDate() {
        val yesterday = LocalDate.of(2026, 9, 7)
        val today = yesterday.plusDays(1)
        val record = Completion("task-1", yesterday.toString(), 1L)
        assertEquals("2026-09-07", record.localDate)
        assertNotEquals(today.toString(), record.localDate)
    }

    @Test fun entryDoesNotDependOnDailyCompletion() {
        val entry = Entry("task-1", NurKind.AMANAH, "Read Quran", 0, 1L)
        val completion = Completion(entry.id, "2026-09-07", 1L)
        assertEquals("Read Quran", entry.title)
        assertFalse(entry.archived)
        assertNotEquals("2026-09-08", completion.localDate)
    }
}
