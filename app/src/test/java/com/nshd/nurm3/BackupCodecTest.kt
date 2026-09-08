package com.nshd.nurm3

import com.nshd.nurm3.data.*
import org.junit.Assert.*
import org.junit.Test

class BackupCodecTest {
    @Test fun backupRoundTripCanBeInspected() {
        val json = BackupCodec.export(
            entries = listOf(Entry("a1", NurKind.AMANAH, "Read", 0, 1L, startDate = "2026-09-08")),
            completions = listOf(Completion("a1", "2026-09-08", 2L, "Read", NurKind.AMANAH)),
            createdAt = 3L
        )
        val summary = BackupCodec.inspect(json)
        assertEquals(1, summary.schema)
        assertEquals(3L, summary.createdAt)
        assertEquals(1, summary.entryCount)
        assertEquals(1, summary.completionCount)
    }
}
