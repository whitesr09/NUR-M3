package com.nshd.nurm3

import com.nshd.nurm3.data.AppearanceChoices
import org.junit.Assert.*
import org.junit.Test

class AppearanceChoicesTest {
    @Test fun everyRequestedThemeAndProgressStyleIsSupported() {
        listOf("system", "light", "dark", "amoled").forEach { assertEquals(it, AppearanceChoices.mode(it)) }
        listOf("slim", "thick", "wavy", "squiggly").forEach { assertEquals(it, AppearanceChoices.progress(it)) }
        assertEquals("dark", AppearanceChoices.mode("unknown"))
        assertEquals("dark", AppearanceChoices.mode(null))
        assertEquals("slim", AppearanceChoices.progress("unknown"))
        assertEquals("slim", AppearanceChoices.progress(null))
    }

    @Test fun refreshSelectionPrefers120WithoutChangingResolution() {
        val current = RefreshCandidate(1, 1080, 2400, 60f)
        val modes = listOf(current, RefreshCandidate(2, 1080, 2400, 90f), RefreshCandidate(3, 1080, 2400, 120f), RefreshCandidate(4, 1080, 2400, 144f), RefreshCandidate(5, 720, 1600, 120f))
        assertEquals(3, RefreshRatePolicy.choose(modes, current)?.id)
        assertEquals(2, RefreshRatePolicy.choose(modes, current, 90f)?.id)
        assertEquals(1, RefreshRatePolicy.choose(listOf(current, modes[4]), current)?.id)
    }

    @Test fun invalidModesAreRejectedAndCurrentModeWinsAnEqualRate() {
        val current = RefreshCandidate(1, 1080, 2400, 120f)
        val modes = listOf(RefreshCandidate(2, 1080, 2400, Float.NaN), RefreshCandidate(3, 1080, 2400, 0f), RefreshCandidate(4, 1080, 2400, 144f))
        assertNull(RefreshRatePolicy.choose(modes, current))
        assertEquals(1, RefreshRatePolicy.choose(modes + RefreshCandidate(5, 1080, 2400, 120f) + current, current)?.id)
    }
}
