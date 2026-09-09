package com.nshd.nurm3.ui

/** Visual-only choices. No completion or entry data is stored here. */
object JourneySize {
    const val COMPACT = "compact"
    const val MEDIUM = "medium"
    const val EXPANDED = "expanded"
    val all = listOf(COMPACT, MEDIUM, EXPANDED)
    fun normalize(value: String?): String = value?.takeIf { it in all } ?: MEDIUM
}

/** Separating presentation from the original layout keeps older saved layouts compatible. */
data class JourneyOptions(
    val sizes: Map<String, String> = emptyMap(),
    val pinned: Set<String> = emptySet(),
    val collapsed: Set<String> = emptySet()
) {
    fun size(id: String): String = JourneySize.normalize(sizes[id])
    fun withSize(id: String, size: String): JourneyOptions =
        if (id !in JourneyCard.all) this else copy(sizes = sizes + (id to JourneySize.normalize(size)))
    fun withPin(id: String, enabled: Boolean): JourneyOptions =
        if (id !in JourneyCard.all) this else copy(pinned = if (enabled) pinned + id else pinned - id)
    fun withCollapsed(id: String, enabled: Boolean): JourneyOptions =
        if (id !in JourneyCard.all) this else copy(collapsed = if (enabled) collapsed + id else collapsed - id)
    fun visible(layout: JourneyLayout): List<String> {
        val ordered = layout.visible()
        return ordered.filter { it in pinned } + ordered.filterNot { it in pinned }
    }
    companion object {
        fun restore(sizes: String?, pinned: String?, collapsed: String?): JourneyOptions {
            val known = JourneyCard.all.toSet()
            val restoredSizes = sizes.orEmpty().split(',').mapNotNull { part ->
                val pieces = part.split(':', limit = 2)
                if (pieces.size == 2 && pieces[0] in known) pieces[0] to JourneySize.normalize(pieces[1]) else null
            }.toMap()
            return JourneyOptions(
                restoredSizes,
                pinned.orEmpty().split(',').filter { it in known }.toSet(),
                collapsed.orEmpty().split(',').filter { it in known }.toSet()
            )
        }
    }
    fun encodeSizes(): String = JourneyCard.all.mapNotNull { id -> sizes[id]?.let { "$id:${JourneySize.normalize(it)}" } }.joinToString(",")
    fun encodePinned(): String = JourneyCard.all.filter { it in pinned }.joinToString(",")
    fun encodeCollapsed(): String = JourneyCard.all.filter { it in collapsed }.joinToString(",")
}

/** A named snapshot of the dashboard, not a snapshot of personal activity. */
data class JourneyPreset(val name: String, val layout: JourneyLayout, val options: JourneyOptions) {
    companion object {
        fun validName(name: String): Boolean = name.trim().length in 1..32 && !name.contains('\n')
    }
}
