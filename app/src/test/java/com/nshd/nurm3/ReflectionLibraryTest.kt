package com.nshd.nurm3

import com.nshd.nurm3.data.ReflectionLibrary
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ReflectionLibraryTest {
    @Test fun referencesAreUniqueAndResolvable() {
        assertEquals(ReflectionLibrary.items.size, ReflectionLibrary.items.map { it.id }.distinct().size)
        ReflectionLibrary.items.forEach { item ->
            assertEquals(item, ReflectionLibrary.find(item.id))
            assertTrue(item.sourceUrl == "https://quran.com/${item.id.replace(':', '/')}")
            assertTrue(item.arabic.isNotBlank())
        }
        assertNull(ReflectionLibrary.find("not-a-verse"))
    }

    @Test fun searchSupportsReferencesAndMeanings() {
        assertEquals(ReflectionLibrary.items, ReflectionLibrary.search("  "))
        assertEquals(listOf("13:28"), ReflectionLibrary.search("13:28").map { it.id })
        assertTrue(ReflectionLibrary.search("mercy").any { it.id == "39:53" })
        assertTrue(ReflectionLibrary.search("no matching passage").isEmpty())
    }

    @Test fun dailySelectionIsStableAndAlwaysInTheCatalog() {
        val date = LocalDate.of(2026, 9, 9)
        assertEquals(ReflectionLibrary.daily(date), ReflectionLibrary.daily(date))
        assertNotEquals(ReflectionLibrary.daily(date), ReflectionLibrary.daily(date.plusDays(1)))
        listOf(date.minusDays(10000), date, date.plusDays(10000)).forEach {
            assertTrue(ReflectionLibrary.daily(it) in ReflectionLibrary.items)
        }
    }
}
