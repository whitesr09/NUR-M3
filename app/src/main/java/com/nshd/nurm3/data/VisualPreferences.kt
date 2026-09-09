package com.nshd.nurm3.data

import com.nshd.nurm3.ui.NurGlassStyle

/** Appearance-only settings. No data or completion records are affected. */
data class VisualPreferences(
    val glass: NurGlassStyle = NurGlassStyle(),
    val ringStyle: String = "slim",
    val barStyle: String = "slim",
    val ringStroke: Float = 1f,
    val barStroke: Float = 1f,
    val motionIntensity: Float = 1f
) {
    companion object {
        fun normalize(glassMode: String?, glassIntensity: Float, glassRadius: Int,
            ring: String?, bar: String?, ringStroke: Float, barStroke: Float, motion: Float) = VisualPreferences(
            NurGlassStyle.normalize(glassMode, glassIntensity, glassRadius),
            AppearanceChoices.progress(ring), AppearanceChoices.progress(bar),
            finite(ringStroke, 0.6f, 1.8f), finite(barStroke, 0.6f, 1.8f), finite(motion, 0f, 1.5f))
        private fun finite(value: Float, min: Float, max: Float) = if (value.isFinite()) value.coerceIn(min, max) else 1f
    }
}
