package com.nshd.nurm3

import com.nshd.nurm3.ui.NurMotion
import org.junit.Assert.assertEquals
import org.junit.Test

class NurMotionTest {
    @Test fun progressIsAlwaysBounded() {
        assertEquals(0f, NurMotion.fraction(-1f), 0f)
        assertEquals(1f, NurMotion.fraction(2f), 0f)
        assertEquals(0f, NurMotion.fraction(Float.NaN), 0f)
        assertEquals(0f, NurMotion.fraction(Float.POSITIVE_INFINITY), 0f)
        assertEquals(0.25f, NurMotion.fraction(0.25f), 0f)
    }

    @Test fun percentagesAreBoundedAndRepresentActualValues() {
        assertEquals(0, NurMotion.percent(Float.NaN))
        assertEquals(0, NurMotion.percent(-1f))
        assertEquals(0, NurMotion.percent(0f))
        assertEquals(50, NurMotion.percent(0.5f))
        assertEquals(100, NurMotion.percent(1f))
        assertEquals(100, NurMotion.percent(4f))
    }
}
