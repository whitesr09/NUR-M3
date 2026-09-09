package com.nshd.nurm3

import com.nshd.nurm3.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class BackupCodecTest {
    private val entry = Entry("a1", NurKind.AMANAH, "Read", 0, 1L, startDate = "2026-09-08")
    private val completion = Completion("a1", "2026-09-08", 2L, "Read", NurKind.AMANAH)
    private val phrase = DhikrPhrase("custom-a", "My remembrance", 33, 7, 0, 1L)
    private val day = DhikrDay("custom-a", "2026-09-08", 12)

    @Test fun roundTripPreservesIdentifiersDatesAndDhikr() {
        val raw = BackupCodec.export(listOf(entry), listOf(completion), 3L, listOf(phrase), listOf(day))
        val payload = BackupCodec.decode(raw)
        val summary = BackupCodec.inspect(raw)
        assertEquals(3, summary.schema)
        assertEquals(1, summary.dhikrPhraseCount)
        assertEquals(1, summary.dhikrDayCount)
        assertEquals(entry, payload.entries.single())
        assertEquals(completion, payload.completions.single())
        assertEquals(phrase, payload.dhikrPhrases.single())
        assertEquals(day, payload.dhikrDays.single())
        assertEquals(3L, payload.createdAt)
        BackupCodec.validate(payload)
    }

    @Test fun nullableDatesRemainNull() {
        val original = entry.copy(startDate = null, endDate = null)
        assertEquals(original, BackupCodec.decode(BackupCodec.export(listOf(original), emptyList())).entries.single())
    }

    @Test fun acceptsVersionsOneAndTwoWithoutDhikr() {
        val current = BackupCodec.export(listOf(entry), listOf(completion))
        for (version in 1..2) {
            val root = JSONObject(current).put("schema", version)
                .remove("dhikrPhrases")
            // JSONObject.remove returns the removed value, so create a separate root below.
            val legacy = JSONObject(current).put("schema", version)
            legacy.remove("dhikrPhrases")
            legacy.remove("dhikrDays")
            val payload = BackupCodec.decode(legacy.toString())
            assertEquals(version, payload.schema)
            assertEquals(entry, payload.entries.single())
            assertTrue(payload.dhikrPhrases.isEmpty())
            assertTrue(payload.dhikrDays.isEmpty())
            BackupCodec.validate(payload)
        }
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsMissingDefinitions() {
        BackupCodec.decode("""{"app":"NUR-M3","schema":2,"entries":[],"completions":[{"entryId":"missing","localDate":"2026-09-08","completedAt":1}]}""")
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsDuplicateCompletions() {
        BackupCodec.decode(BackupCodec.export(listOf(entry), listOf(completion, completion)))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsDuplicateDhikrPhrases() {
        BackupCodec.decode(BackupCodec.export(emptyList(), emptyList(), dhikrPhrases = listOf(phrase, phrase)))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsOrphanDhikrDays() {
        BackupCodec.decode(BackupCodec.export(emptyList(), emptyList(), dhikrDays = listOf(day)))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsNegativeDhikrCounts() {
        BackupCodec.decode(BackupCodec.export(emptyList(), emptyList(), dhikrPhrases = listOf(phrase), dhikrDays = listOf(day.copy(count = -1))))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsDuplicateDhikrDays() {
        BackupCodec.decode(BackupCodec.export(emptyList(), emptyList(), dhikrPhrases = listOf(phrase), dhikrDays = listOf(day, day)))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsMissingVersionThreeSections() {
        val root = JSONObject(BackupCodec.export(emptyList(), emptyList()))
        root.remove("dhikrDays")
        BackupCodec.decode(root.toString())
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsUnsupportedFutureSchema() {
        BackupCodec.decode("""{"app":"NUR-M3","schema":99,"entries":[],"completions":[]}""")
    }
}
