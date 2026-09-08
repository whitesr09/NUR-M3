package com.nshd.nurm3.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.*

/** Animations are presentation only; progress values always come from persisted records. */
@Composable
fun NurAnimatedFraction(target: Float, reduceMotion: Boolean, label: String): Float {
    val value by animateFloatAsState(
        targetValue = target.coerceIn(0f, 1f),
        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = label
    )
    return value
}

@Composable
fun NurLinearProgress(progress: Float, description: String, reduceMotion: Boolean, modifier: Modifier = Modifier) {
    val target = progress.coerceIn(0f, 1f)
    val animated = NurAnimatedFraction(target, reduceMotion, description)
    LinearProgressIndicator(
        progress = { animated },
        modifier = modifier.fillMaxWidth().semantics { contentDescription = description; progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f) },
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

@Composable
fun NurCircularProgress(progress: Float, description: String, reduceMotion: Boolean, modifier: Modifier = Modifier, strokeWidth: Dp = 8.dp, color: Color = MaterialTheme.colorScheme.primary) {
    val target = progress.coerceIn(0f, 1f)
    val animated = NurAnimatedFraction(target, reduceMotion, description)
    CircularProgressIndicator(
        progress = { animated }, modifier = modifier.semantics { contentDescription = description; progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f) },
        strokeWidth = strokeWidth, color = color, trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}
