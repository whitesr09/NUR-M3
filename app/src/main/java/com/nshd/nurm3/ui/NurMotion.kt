package com.nshd.nurm3.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

val LocalNurReduceMotion = staticCompositionLocalOf { false }
object NurMotion {
    fun fraction(value: Float): Float = if (value.isFinite()) value.coerceIn(0f, 1f) else 0f
    fun percent(value: Float): Int = (fraction(value) * 100f).roundToInt().coerceIn(0, 100)
}
@Composable
fun rememberNurProgress(target: Float, date: LocalDate, label: String): Float {
    val value = NurMotion.fraction(target)
    val reduce = LocalNurReduceMotion.current
    val intensity = LocalNurMotionIntensity.current.coerceIn(0f, 1.5f)
    val state = remember(date, label) { Animatable(value) }
    LaunchedEffect(state, value, reduce, intensity) {
        if (reduce || intensity == 0f) state.snapTo(value)
        else state.animateTo(value, tween((360 * intensity).roundToInt().coerceAtLeast(1), easing = FastOutSlowInEasing))
    }
    return state.value
}
private fun wavePath(width: Float, center: Float, amplitude: Float, cycles: Float, end: Float): Path = Path().apply {
    moveTo(0f, center)
    val steps = 80
    for (i in 1..steps) {
        val x = end * i / steps
        val y = center + sin(2.0 * PI * cycles * x / width).toFloat() * amplitude
        lineTo(x, y)
    }
}
@Composable
fun NurLinearProgress(target: Float, date: LocalDate, label: String, modifier: Modifier = Modifier, style: String = LocalNurBarStyle.current) {
    val value = NurMotion.fraction(target)
    val animated = rememberNurProgress(value, date, label)
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    val multiplier = LocalNurBarStroke.current.coerceIn(0.6f, 1.8f)
    val height = when (style) { "thick" -> 13.dp * multiplier; "wavy", "squiggly" -> 22.dp * multiplier; else -> 6.dp * multiplier }
    val stroke = when (style) { "thick" -> 13.dp * multiplier; "wavy", "squiggly" -> 5.dp * multiplier; else -> 6.dp * multiplier }
    if (style == "wavy" || style == "squiggly") {
        Canvas(modifier.fillMaxWidth().height(height).clearAndSetSemantics {
            contentDescription = label
            progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
        }) {
            val y = size.height / 2f
            val amplitude = if (style == "squiggly") 5.dp.toPx() * multiplier else 3.dp.toPx() * multiplier
            val cycles = if (style == "squiggly") 5f else 2f
            drawPath(wavePath(size.width, y, amplitude, cycles, size.width), scheme.surfaceVariant, style = Stroke(stroke.toPx(), cap = StrokeCap.Round))
            if (animated > 0f) drawPath(wavePath(size.width, y, amplitude, cycles, size.width * animated), scheme.primary, style = Stroke(stroke.toPx(), cap = StrokeCap.Round))
        }
    } else {
        Box(modifier.fillMaxWidth().height(height).clip(shape).background(scheme.surfaceVariant)
            .clearAndSetSemantics { contentDescription = label; progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f) }) {
            Box(Modifier.fillMaxWidth(animated).fillMaxHeight().background(scheme.primary, shape))
        }
    }
}
@Composable
fun NurCircularProgress(target: Float, date: LocalDate, label: String, modifier: Modifier = Modifier, strokeWidth: Dp = 9.dp, style: String = LocalNurRingStyle.current, color: Color = MaterialTheme.colorScheme.primary, trackColor: Color = MaterialTheme.colorScheme.surfaceVariant) {
    val value = NurMotion.fraction(target)
    val animated = rememberNurProgress(value, date, label)
    val multiplier = LocalNurRingStroke.current.coerceIn(0.6f, 1.8f)
    val width = (when (style) { "slim" -> (strokeWidth.value * 0.65f).coerceAtLeast(2f).dp; "thick" -> strokeWidth * 1.45f; else -> strokeWidth }) * multiplier
    Canvas(modifier.clearAndSetSemantics { contentDescription = label; progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f) }) {
        val strokePx = width.toPx()
        val amplitude = when (style) { "squiggly" -> strokePx * 0.35f; "wavy" -> strokePx * 0.22f; else -> 0f }
        val radius = (size.minDimension - strokePx - 2f * amplitude) / 2f
        if (radius <= 0f) return@Canvas
        val center = this.center
        val cycles = if (style == "squiggly") 16 else 8
        fun ring(end: Float): Path = Path().apply {
            val segments = 240
            for (i in 0..segments) {
                val fraction = i.toFloat() / segments
                val angle = -PI / 2.0 + 2.0 * PI * end * fraction
                val r = radius + sin(2.0 * PI * cycles * end * fraction).toFloat() * amplitude
                val x = center.x + cos(angle).toFloat() * r
                val y = center.y + sin(angle).toFloat() * r
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(ring(1f), trackColor, style = Stroke(strokePx, cap = StrokeCap.Round))
        if (animated > 0f) drawPath(ring(animated), color, style = Stroke(strokePx, cap = StrokeCap.Round))
    }
}
