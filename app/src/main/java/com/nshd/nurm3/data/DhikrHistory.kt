package com.nshd.nurm3.data

import java.time.LocalDate

/** Read-only projections of recorded days. No missing day is ever synthesized. */
data class DhikrHistoryRow(val date: LocalDate, val count: Long)

data class DhikrHistorySummary(
    val recordedDays: Int,
    val total: Long,
    val lastRecordedDate: LocalDate?
)

object DhikrHistory {
    fun rows(days: List<DhikrDay>, phraseId: String): List<DhikrHistoryRow> =
        days.asSequence()
            .filter { it.phraseId == phraseId && it.count > 0 }
            .mapNotNull { day ->
                val date = runCatching { LocalDate.parse(day.localDate) }.getOrNull()
                date?.let { DhikrHistoryRow(it, day.count) }
            }
            .sortedByDescending { it.date }
            .toList()

    fun summary(rows: List<DhikrHistoryRow>): DhikrHistorySummary = DhikrHistorySummary(
        recordedDays = rows.size,
        total = rows.fold(0L) { total, row -> Math.addExact(total, row.count) },
        lastRecordedDate = rows.maxOfOrNull { it.date }
    )

    fun recent(rows: List<DhikrHistoryRow>, today: LocalDate, days: Long = 30): List<DhikrHistoryRow> {
        require(days > 0)
        val first = today.minusDays(days - 1)
        return rows.filter { !it.date.isBefore(first) && !it.date.isAfter(today) }
    }
}
