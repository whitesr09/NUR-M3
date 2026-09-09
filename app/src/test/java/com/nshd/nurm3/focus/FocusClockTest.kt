package com.nshd.nurm3.focus

import org.junit.Assert.*
import org.junit.Test

class FocusClockTest {
    @Test fun pauseAndResumeExcludeInactiveTime() {
        val started = FocusClock(60_000).start(1000)
        val paused = started.pause(11_000)
        assertEquals(10_000L, paused.elapsed(900_000))
        assertEquals(40_000L, paused.start(20_000).elapsed(50_000))
    }
    @Test fun completionCannotBeAwardedByOpeningATimer() {
        val clock = FocusClock(60_000)
        assertFalse(clock.tick(1_000_000).completed)
        assertFalse(clock.start(0).tick(59_999).completed)
        assertTrue(clock.start(0).tick(60_000).completed)
    }
    @Test fun recoveryDoesNotCreditAbsence() {
        val recovered = FocusClock.recover(60_000, 15_000)
        assertEquals(45_000L, recovered.remaining(999_999_999))
        assertEquals(null, recovered.runningSince)
    }
    @Test fun elapsedIsBoundedAndNeverNegative() {
        val clock = FocusClock(60_000).start(1000)
        assertEquals(0L, clock.elapsed(500))
        assertEquals(60_000L, clock.elapsed(Long.MAX_VALUE))
    }
}
