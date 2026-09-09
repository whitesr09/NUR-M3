package com.nshd.nurm3.data

import com.nshd.nurm3.ui.DailyProgress
import com.nshd.nurm3.ui.ProgressSummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A bounded review of genuine records. Archived records are never silently discarded. */
data class ReviewDay(
    val date: LocalDate,
    val scheduled: ProgressSummary,
    val recordedCount: Int
)

data class ReviewSummary(
    val completed: Int,
    val scheduled: Int,
    val recorded: Int,
    val recordedDays: Int,
    val perfectDays: Int,
    val days: Int
) {
    val fraction: Float get() = if (scheduled == 0) 0f else completed.toFloat() / scheduled
}

data class ReviewResult(val days: List<ReviewDay>, val summary: ReviewSummary)

object ReviewInsights {
    private val kinds = setOf(NurKind.PRAYER, NurKind.AMANAH, NurKind.MUHASABA, NurKind.RHYTHM)

    /** The first known creation or recorded date, never an assumed installation date. */
    fun firstKnownDate(entries: List<Entry>, completions: List<Completion>, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): LocalDate? {
        val created = entries.asSequence().filter { it.createdAt > 0L }
            .mapNotNull { runCatching { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() }.getOrNull() }
        val recorded = completions.asSequence().mapNotNull { runCatching { LocalDate.parse(it.localDate) }.getOrNull() }
        return (created + recorded).filter { !it.isAfter(today) }.minOrNull()
    }

    fun review(
        entries: List<Entry>, completions: List<Completion>, today: LocalDate,
        lookbackDays: Int = 7, kind: String = "all", zone: ZoneId = ZoneId.systemDefault()
    ): ReviewResult {
        require(lookbackDays in 1..365) { "Review range must be between 1 and 365 days" }
        require(kind == "all" || kind in kinds) { "Unknown activity type" }
        val first = firstKnownDate(entries, completions, today, zone)
            ?: return ReviewResult(emptyList(), ReviewSummary(0, 0, 0, 0, 0, 0))
        val start = maxOf(first, today.minusDays((lookbackDays - 1).toLong()))
        val definitions = entries.filter { kind == "all" || it.kind == kind }
        val entryKinds = entries.associate { it.id to it.kind }
        val records = completions.asSequence().mapNotNull { record ->
            val date = runCatching { LocalDate.parse(record.localDate) }.getOrNull() ?: return@mapNotNull null
            if (date.isBefore(start) || date.isAfter(today)) return@mapNotNull null
            val recordKind = record.kindSnapshot.ifBlank { entryKinds[record.entryId].orEmpty() }
            if (kind != "all" && recordKind != kind) return@mapNotNull null
            Triple(record.entryId, date, recordKind)
        }.distinctBy { it.first to it.second }.groupingBy { it.second }.eachCount()
        val days = NurInsights.daily(definitions, completions, start, today, zone).map {
            ReviewDay(it.date, it.summary, records[it.date] ?: 0)
        }
        return ReviewResult(days, summarize(days))
    }

    fun summarize(days: List<ReviewDay>): ReviewSummary = ReviewSummary(
        completed = days.sumOf { it.scheduled.completed },
        scheduled = days.sumOf { it.scheduled.total },
        recorded = days.sumOf { it.recordedCount },
        recordedDays = days.count { it.recordedCount > 0 },
        perfectDays = days.count { it.scheduled.total > 0 && it.scheduled.completed == it.scheduled.total },
        days = days.size
    )
}
