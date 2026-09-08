package com.nshd.nurm3

import com.nshd.nurm3.data.*
import org.junit.Assert.*
import org.junit.Test

class BackupCodecTest {
    private val entry = Entry("a1", NurKind.AMANAH, "Read", 0, 1L, startDate = "2026-09-08")
    private val completion = Completion("a1", "2026-09-08", 2L, "Read", NurKind.AMANAH)
    @Test fun roundTripPreservesIdentifiersAndDates() {
        val raw = BackupCodec.export(listOf(entry), listOf(completion), 3L)
        val payload = BackupCodec.decode(raw)
        assertEquals(2, BackupCodec.inspect(raw).schema)
        assertEquals(entry, payload.entries.single())
        assertEquals(completion, payload.completions.single())
        assertEquals(3L, payload.createdAt)
    }
    @Test fun acceptsVersionOne() {
        val raw = BackupCodec.export(listOf(entry), listOf(completion)).replace("\"schema\": 2", "\"schema\": 1")
        assertEquals(1, BackupCodec.decode(raw).entries.size)
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsMissingDefinitions() {
        BackupCodec.decode("""{"app":"NUR-M3","schema":2,"entries":[],"completions":[{"entryId":"missing","localDate":"2026-09-08","completedAt":1}]}""")
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsDuplicateCompletions() {
        BackupCodec.decode(BackupCodec.export(listOf(entry), listOf(completion, completion)))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsUnsupportedFutureSchema() {
        BackupCodec.decode("""{"app":"NUR-M3","schema":99,"entries":[],"completions":[]}""")
    }
}
