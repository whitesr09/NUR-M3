package com.nshd.nurm3

import android.view.Display

/** Requests only modes the display reports, retaining the current resolution. */
object RefreshRatePolicy {
    fun choose(modes: Array<Display.Mode>, current: Display.Mode, ceiling: Float = 120f): Display.Mode? {
        return modes.asSequence()
            .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
            .filter { it.refreshRate.isFinite() && it.refreshRate > 0f && it.refreshRate <= ceiling + 0.1f }
            .maxWithOrNull(compareBy<Display.Mode> { it.refreshRate }.thenBy { if (it.modeId == current.modeId) 1 else 0 })
    }
}
