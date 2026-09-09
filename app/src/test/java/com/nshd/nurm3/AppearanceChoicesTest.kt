package com.nshd.nurm3.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceChoicesTest {
    @Test fun modesAreValidated() {
        assertEquals("amoled", AppearanceChoices.mode("amoled"))
        assertEquals("dark", AppearanceChoices.mode("unknown"))
        assertEquals("dark", AppearanceChoices.mode(null))
    }
    @Test fun progressStylesAreValidated() {
        assertEquals("squiggly", AppearanceChoices.progress("squiggly"))
        assertEquals("slim", AppearanceChoices.progress("invalid"))
        assertEquals("slim", AppearanceChoices.progress(null))
    }
}
