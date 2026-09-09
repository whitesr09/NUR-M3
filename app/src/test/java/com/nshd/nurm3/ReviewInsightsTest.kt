package com.nshd.nurm3

import com.nshd.nurm3.data.*
import com.nshd.nurm3.ui.ProgressSummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class ReviewInsightsTest {
    private val today = LocalDate.parse("2026-09-09")
    private val zone = ZoneOffset.UTC
    private fun millis(date: String): Long = LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli()
    private fun entry(id: String, kind: String = NurKind.AMANAH, created: String = "2026-09-07", archived: Boolean = false) =
        Entry(id, kind, id, 0, millis(created), archived = archived, startDate = created)
    private fun record(id: String, date: String, kind: String = NurKind.AMANAH) =
        Completion(id, date, millis(date), id, kind)

    @Test fun emptyDatabaseDoesNotInventHistoricalDays() {
        val result = ReviewInsights.review(emptyList(), emptyList(), today, 90, zone = zone)
        assertTrue(result.days.isEmpty())
        assertEquals(0, result.summary.days)
        assertEquals(0f, result.summary.fraction)
    }

    @Test fun reviewStartsAtFirstKnownActivityAndPreservesOlderRecords() {
        val entries = listOf(entry("a"))
        val records = listOf(record("a", "2026-09-06"), record("a", "2026-09-08"))
        val result = ReviewInsights.review(entries, records, today, 30, zone = zone)
        assertEquals(LocalDate.parse("2026-09-06"), result.days.first().date)
        assertEquals(4, result.summary.days)
        assertEquals(3, result.summary.scheduled)
        assertEquals(1, result.summary.completed)
        assertEquals(2, result.summary.recorded)
        assertEquals(2, result.summary.recordedDays)
    }

    @Test fun archivedAndFutureRecordsAreHandledWithoutInflatingProgress() {
        val entries = listOf(entry("a"), entry("b", NurKind.RHYTHM, archived = true))
        val records = listOf(record("a", "2026-09-08"), record("b", "2026-09-08", NurKind.RHYTHM), record("a", "2026-09-10"))
        val result = ReviewInsights.review(entries, records, today, 7, zone = zone)
        assertEquals(2, result.summary.recorded)
        assertEquals(1, result.summary.completed)
        assertEquals(3, result.summary.scheduled)
        assertEquals(1, result.summary.recordedDays)
        assertEquals(0, result.days.last().recordedCount)
        assertEquals(1, ReviewInsights.review(entries, records, today, 7, NurKind.RHYTHM, zone).summary.recorded)
        assertEquals(0, ReviewInsights.review(entries, records, today, 7, NurKind.RHYTHM, zone).summary.scheduled)
    }

    @Test fun duplicateRecordsDoNotInflateRecordedCounts() {
        val records = listOf(record("a", "2026-09-08"), record("a", "2026-09-08"))
        val result = ReviewInsights.review(listOf(entry("a")), records, today, zone = zone)
        assertEquals(1, result.summary.recorded)
        assertEquals(1, result.summary.completed)
    }

    @Test fun summariesCountOnlyActualPerfectDays() {
        val days = listOf(
            ReviewDay(today.minusDays(2), ProgressSummary(0, 0), 0),
            ReviewDay(today.minusDays(1), ProgressSummary(2, 2), 2),
            ReviewDay(today, ProgressSummary(1, 3), 1)
        )
        val summary = ReviewInsights.summarize(days)
        assertEquals(3, summary.completed)
        assertEquals(5, summary.scheduled)
        assertEquals(3, summary.recorded)
        assertEquals(2, summary.recordedDays)
        assertEquals(1, summary.perfectDays)
        assertEquals(0.6f, summary.fraction, 0.0001f)
    }

    @Test fun unsupportedRangesAndKindsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { ReviewInsights.review(emptyList(), emptyList(), today, 366) }
        assertThrows(IllegalArgumentException::class.java) { ReviewInsights.review(emptyList(), emptyList(), today, kind = "unknown") }
    }
}
