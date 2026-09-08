package com.nshd.nurm3.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Pure scheduling rules. A day is selected by date, never by clearing stored definitions. */
object EntrySchedule {
    fun isActive(entry: Entry, date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        if (entry.archived) return false
        if (entry.kind == NurKind.PRAYER) return true
        val start = entry.startDate?.let { LocalDate.parse(it) }
            ?: Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate()
        if (date.isBefore(start)) return false
        if (entry.endDate != null && date.isAfter(LocalDate.parse(entry.endDate))) return false
        return when (entry.schedule) {
            "once" -> date == start
            "weekly" -> date.dayOfWeek == start.dayOfWeek
            "weekdays" -> entry.weekdaysMask and (1 shl (date.dayOfWeek.value - 1)) != 0
            else -> true
        }
    }
}
