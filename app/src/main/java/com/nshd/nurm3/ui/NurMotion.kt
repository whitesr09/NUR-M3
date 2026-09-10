package com.nshd.nurm3.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

/**
 * Short, deterministic progress motion avoids long-running springs and keeps multiple dashboard
 * progress indicators from competing for frame time after a completion change.
 */
@Composable
fun rememberNurProgress(target: Float, date: LocalDate, label: String): Float {
    val value = NurMotion.fraction(target)
    val reduceMotion = LocalNurReduceMotion.current
    val animated by animateFloatAsState(
        targetValue = value,
        animationSpec = if (reduceMotion) snap() else tween(
            durationMillis = 220,
            easing = FastOutSlowInEasing
        ),
        label = "NUR progress"
    )
    return animated
}

private fun wavePath(width: Float, center: Float, amplitude: Float, cycles: Float, end: Float): Path = Path().apply {
    moveTo(0f, center)
    // 48 points is visually smooth at phone widths while being substantially cheaper than 80.
    val steps = 48
    for (i in 1..steps) {
        val x = end * i / steps
        val y = center + sin(2.0 * PI * cycles * x / width).toFloat() * amplitude
        lineTo(x, y)
    }
}

@Composable
fun NurLinearProgress(
    target: Float,
    date: LocalDate,
    label: String,
    modifier: Modifier = Modifier,
    style: String = LocalNurProgressStyle.current
) {
    val value = NurMotion.fraction(target)
    val animated = rememberNurProgress(value, date, label)
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    val height = when (style) { "thick" -> 13.dp; "wavy", "squiggly" -> 22.dp; else -> 6.dp }
    val stroke = when (style) { "thick" -> 13.dp; "wavy", "squiggly" -> 5.dp; else -> 6.dp }

    if (style == "wavy" || style == "squiggly") {
        Canvas(
            modifier.fillMaxWidth().height(height).clearAndSetSemantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
            }
        ) {
            val y = size.height / 2f
            val amplitude = if (style == "squiggly") 5.dp.toPx() else 3.dp.toPx()
            val cycles = if (style == "squiggly") 5f else 2f
            val track = wavePath(size.width, y, amplitude, cycles, size.width)
            drawPath(track, scheme.surfaceVariant, style = Stroke(stroke.toPx(), cap = StrokeCap.Round))
            if (animated > 0f) {
                val filled = wavePath(size.width, y, amplitude, cycles, size.width * animated)
                drawPath(filled, scheme.primary, style = Stroke(stroke.toPx(), cap = StrokeCap.Round))
            }
        }
    } else {
        Box(
            modifier.fillMaxWidth().height(height).clip(shape).background(scheme.surfaceVariant)
                .clearAndSetSemantics {
                    contentDescription = label
                    progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
                }
        ) {
            Box(Modifier.fillMaxWidth(animated).fillMaxHeight().background(scheme.primary, shape))
        }
    }
}

@Composable
fun NurCircularProgress(
    target: Float,
    date: LocalDate,
    label: String,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 9.dp,
    style: String = LocalNurProgressStyle.current,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val value = NurMotion.fraction(target)
    val animated = rememberNurProgress(value, date, label)
    val width = when (style) {
        "slim" -> (strokeWidth.value * 0.65f).coerceAtLeast(2f).dp
        "thick" -> (strokeWidth.value * 1.45f).dp
        else -> strokeWidth
    }

    Canvas(
        modifier.clearAndSetSemantics {
            contentDescription = label
            progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
        }
    ) {
        val strokePx = width.toPx()
        val radius = (size.minDimension - strokePx) / 2f
        if (radius <= 0f) return@Canvas
        val center = this.center
        val squiggle = style == "squiggly"
        val wavy = style == "wavy"
        val amplitude = if (squiggle) strokePx * 0.35f else if (wavy) strokePx * 0.22f else 0f
        val cycles = if (squiggle) 16 else 8
        val segments = if (squiggle) 144 else if (wavy) 112 else 96

        fun ring(end: Float): Path = Path().apply {
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
        if (animated > 0f) {
            drawPath(ring(animated), color, style = Stroke(strokePx, cap = StrokeCap.Round))
        }
    }
}
