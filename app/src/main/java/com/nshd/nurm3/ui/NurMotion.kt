package com.nshd.nurm3.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalMotionDurationScale
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import java.time.LocalDate
import kotlin.math.roundToInt

/** All decorative motion is optional. Data and accessibility never depend on animation. */
val LocalNurReduceMotion = staticCompositionLocalOf { false }

object NurMotion {
    fun fraction(value: Float): Float = if (value.isFinite()) value.coerceIn(0f, 1f) else 0f
    fun percent(value: Float): Int = (fraction(value) * 100f).roundToInt().coerceIn(0, 100)
}

/**
 * Each indicator owns its own Animatable and is keyed to its local date.
 * Recreating the state for a new day starts at the new day's real value, not
 * yesterday's percentage. A new target cancels an in-flight animation.
 */
@Composable
fun rememberNurProgress(target: Float, date: LocalDate, label: String): Float {
    val value = NurMotion.fraction(target)
    val reduceMotion = LocalNurReduceMotion.current || LocalMotionDurationScale.current.scaleFactor <= 0f
    val state = remember(date, label) { Animatable(value) }
    LaunchedEffect(state, value, reduceMotion) {
        if (reduceMotion) state.snapTo(value)
        else state.animateTo(value, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow))
    }
    return state.value
}

@Composable
fun NurLinearProgress(target: Float, date: LocalDate, label: String, modifier: Modifier = Modifier) {
    val value = NurMotion.fraction(target)
    val animated = rememberNurProgress(value, date, label)
    LinearProgressIndicator(
        progress = { animated },
        modifier = modifier.fillMaxWidth().clearAndSetSemantics {
            contentDescription = label
            progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
        },
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}
