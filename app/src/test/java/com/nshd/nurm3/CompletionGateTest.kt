package com.nshd.nurm3

import com.nshd.nurm3.data.CompletionGate
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class CompletionGateTest {
    @Test fun duplicateRequestsAreRejectedUntilReleased() {
        val gate = CompletionGate()
        val key = CompletionGate.key("prayer-1", LocalDate.of(2026, 9, 9))
        assertTrue(gate.acquire(key))
        assertTrue(gate.contains(key))
        assertFalse(gate.acquire(key))
        gate.release(key)
        assertFalse(gate.contains(key))
        assertTrue(gate.acquire(key))
    }

    @Test fun differentEntriesAndDatesDoNotBlockEachOther() {
        val gate = CompletionGate()
        val today = LocalDate.of(2026, 9, 9)
        val first = CompletionGate.key("task", today)
        val tomorrow = CompletionGate.key("task", today.plusDays(1))
        val other = CompletionGate.key("other", today)
        assertTrue(gate.acquire(first))
        assertTrue(gate.acquire(tomorrow))
        assertTrue(gate.acquire(other))
        assertFalse(gate.acquire(first))
        gate.release(first)
        assertTrue(gate.contains(tomorrow))
        assertTrue(gate.contains(other))
    }

    @Test fun releaseIsSafeAfterARejectedOrFailedOperation() {
        val gate = CompletionGate()
        val key = CompletionGate.key("task", LocalDate.of(2026, 9, 9))
        gate.release(key)
        assertTrue(gate.acquire(key))
        try {
            throw IllegalStateException("Simulated persistence failure")
        } catch (_: IllegalStateException) {
            gate.release(key)
        }
        assertTrue(gate.acquire(key))
    }
}
