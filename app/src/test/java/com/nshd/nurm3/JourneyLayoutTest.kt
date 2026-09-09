package com.nshd.nurm3

import com.nshd.nurm3.ui.*
import org.junit.Assert.*
import org.junit.Test

class JourneyLayoutTest {
    @Test fun restoreIgnoresUnknownAndDuplicateCards() {
        val layout = JourneyLayout.restore("prayers,unknown,prayers,light", "amanah,unknown")
        assertEquals(JourneyCard.all.size, layout.order.size)
        assertEquals(JourneyCard.PRAYERS, layout.order.first())
        assertFalse(JourneyCard.AMANAH in layout.visible())
    }

    @Test fun movingAtBoundaryDoesNothing() {
        assertEquals(JourneyLayout.DEFAULT, JourneyLayout.DEFAULT.move(JourneyCard.LIGHT, -1))
        assertEquals(JourneyCard.PRAYERS, JourneyLayout.DEFAULT.move(JourneyCard.PRAYERS, -1).order.first())
    }

    @Test fun hidingDoesNotRemoveStoredOrder() {
        val hidden = JourneyLayout.DEFAULT.show(JourneyCard.AMANAH, false)
        assertEquals(JourneyCard.all, hidden.order)
        assertFalse(JourneyCard.AMANAH in hidden.visible())
        assertTrue(JourneyCard.AMANAH in hidden.show(JourneyCard.AMANAH, true).visible())
    }

    @Test fun lastVisibleCardCannotBeHidden() {
        var layout = JourneyLayout.DEFAULT
        JourneyCard.all.drop(1).forEach { layout = layout.show(it, false) }
        assertEquals(listOf(JourneyCard.LIGHT), layout.visible())
        assertEquals(layout, layout.show(JourneyCard.LIGHT, false))
        assertEquals(JourneyCard.all, layout.show(JourneyCard.AMANAH, true).order)
    }

    @Test fun invalidSavedLayoutRecoversWithoutDiscardingOrder() {
        val layout = JourneyLayout.restore("rhythm,amanah,light", JourneyCard.all.joinToString(","))
        assertEquals(JourneyCard.RHYTHM, layout.order.first())
        assertEquals(listOf(JourneyCard.LIGHT), layout.visible())
        assertEquals(JourneyCard.all.size, layout.order.size)
    }
}
