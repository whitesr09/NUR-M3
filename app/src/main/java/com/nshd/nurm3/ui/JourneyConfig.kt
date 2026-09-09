package com.nshd.nurm3.ui

/** Stable IDs allow layouts to survive application upgrades. */
object JourneyCard {
    const val LIGHT = "light"
    const val PRAYERS = "prayers"
    const val AMANAH = "amanah"
    const val MUHASABA = "muhasaba"
    const val RHYTHM = "rhythm"
    val all = listOf(LIGHT, PRAYERS, AMANAH, MUHASABA, RHYTHM)
    val titles = mapOf(LIGHT to "Daily Light", PRAYERS to "Prayers", AMANAH to "Amanah", MUHASABA to "Muhasaba", RHYTHM to "Rhythm")
}

data class JourneyLayout(val order: List<String>, val hidden: Set<String>) {
    fun visible(): List<String> = order.filterNot { it in hidden }
    fun move(id: String, offset: Int): JourneyLayout {
        val index = order.indexOf(id)
        if (index < 0 || index + offset !in order.indices) return this
        val next = order.toMutableList()
        next.removeAt(index)
        next.add(index + offset, id)
        return copy(order = next)
    }
    fun show(id: String, enabled: Boolean): JourneyLayout {
        if (id !in order) return this
        if (!enabled && id !in hidden && visible().size <= 1) return this
        return copy(hidden = if (enabled) hidden - id else hidden + id)
    }

    companion object {
        val DEFAULT = JourneyLayout(JourneyCard.all, emptySet())
        val PRAYER_FIRST = JourneyLayout(listOf(JourneyCard.PRAYERS, JourneyCard.LIGHT, JourneyCard.AMANAH, JourneyCard.MUHASABA, JourneyCard.RHYTHM), emptySet())
        val FOCUS = JourneyLayout(listOf(JourneyCard.AMANAH, JourneyCard.LIGHT, JourneyCard.PRAYERS, JourneyCard.RHYTHM, JourneyCard.MUHASABA), emptySet())
        fun restore(order: String?, hidden: String?): JourneyLayout {
            val known = JourneyCard.all
            val saved = order.orEmpty().split(',').filter { it in known }.distinct()
            val savedHidden = hidden.orEmpty().split(',').filter { it in known }.toSet()
            val normalized = JourneyLayout(saved + known.filterNot { it in saved }, savedHidden)
            return if (normalized.visible().isEmpty()) normalized.copy(hidden = normalized.hidden - JourneyCard.LIGHT) else normalized
        }
    }
}
