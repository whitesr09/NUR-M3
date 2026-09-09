package com.nshd.nurm3

import android.view.Display

/** Pure policy keeps a display's resolution and selects a reported mode at or below 120 Hz. */
data class RefreshCandidate(val id: Int, val width: Int, val height: Int, val rate: Float)

object RefreshRatePolicy {
    fun choose(candidates: List<RefreshCandidate>, current: RefreshCandidate, ceiling: Float = 120f): RefreshCandidate? {
        return candidates.asSequence()
            .filter { it.width == current.width && it.height == current.height }
            .filter { it.rate.isFinite() && it.rate > 0f && it.rate <= ceiling + 0.1f }
            .maxWithOrNull(compareBy<RefreshCandidate> { it.rate }.thenBy { if (it.id == current.id) 1 else 0 })
    }

    fun choose(modes: Array<Display.Mode>, current: Display.Mode, ceiling: Float = 120f): Display.Mode? {
        fun Display.Mode.candidate() = RefreshCandidate(modeId, physicalWidth, physicalHeight, refreshRate)
        val selected = choose(modes.map { it.candidate() }, current.candidate(), ceiling) ?: return null
        return modes.firstOrNull { it.modeId == selected.id }
    }
}
