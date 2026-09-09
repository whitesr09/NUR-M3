package com.nshd.nurm3.ui

import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/** Optional surface system used throughout NUR. */
data class NurGlassStyle(
    val mode: String = "frosted",
    val intensity: Float = 0.66f,
    val blurRadius: Int = 32
) {
    val enabled get() = mode != "off"
    val liquid get() = mode == "liquid"
    val cornerRadius get() = when (mode) {
        "liquid" -> 28
        "frosted" -> 24
        "subtle" -> 20
        else -> 18
    }

    companion object {
        fun normalize(mode: String?, intensity: Float, blurRadius: Int) = NurGlassStyle(
            mode?.takeIf { it in setOf("off", "subtle", "frosted", "liquid") } ?: "frosted",
            if (intensity.isFinite()) intensity.coerceIn(0.25f, 1f) else 0.66f,
            blurRadius.coerceIn(0, 72)
        )
    }
}

val LocalNurGlass = staticCompositionLocalOf { NurGlassStyle() }

/**
 * Adds visible depth behind translucent panels. Compose cannot perform true per-card backdrop
 * filtering on every Android version, so NUR uses real window blur where supported plus a
 * blurred Material-color depth field behind glass surfaces.
 */
@Composable
fun NurGlassBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val style = LocalNurGlass.current
    val scheme = MaterialTheme.colorScheme
    Box(modifier.background(scheme.background)) {
        if (style.enabled) {
            val depth = when (style.mode) {
                "liquid" -> 0.24f
                "frosted" -> 0.17f
                else -> 0.10f
            } * style.intensity
            Canvas(
                Modifier.matchParentSize().blur((style.blurRadius.coerceAtLeast(12)).dp)
            ) {
                val min = size.minDimension
                drawCircle(
                    color = scheme.primary.copy(alpha = depth),
                    radius = min * 0.52f,
                    center = Offset(size.width * 0.88f, size.height * 0.16f)
                )
                drawCircle(
                    color = scheme.tertiary.copy(alpha = depth * 0.72f),
                    radius = min * 0.42f,
                    center = Offset(size.width * 0.08f, size.height * 0.62f)
                )
                drawCircle(
                    color = scheme.secondary.copy(alpha = depth * 0.48f),
                    radius = min * 0.34f,
                    center = Offset(size.width * 0.76f, size.height * 0.88f)
                )
            }
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        listOf(
                            scheme.background.copy(alpha = if (style.liquid) 0.55f else 0.70f),
                            scheme.background.copy(alpha = 0.86f),
                            scheme.background
                        )
                    )
                )
            )
        }
        content()
    }
}

@Composable
fun NurGlassSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val style = LocalNurGlass.current
    val colors = MaterialTheme.colorScheme
    val compact = LocalNurCompact.current
    val reduceMotion = LocalNurReduceMotion.current
    val shape = RoundedCornerShape(style.cornerRadius.dp)

    if (!style.enabled) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.65f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(Modifier.padding(if (compact) 14.dp else 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
        return
    }

    val dark = colors.background.luminance() < 0.35f
    val baseAlpha = when (style.mode) {
        "subtle" -> 0.84f
        "frosted" -> 0.63f
        else -> 0.46f
    }
    val fill = colors.surface.copy(alpha = (baseAlpha * style.intensity + 0.12f).coerceIn(0.28f, 0.94f))
    val border = if (dark) Color.White.copy(alpha = if (style.liquid) 0.22f else 0.14f)
        else Color.White.copy(alpha = if (style.liquid) 0.82f else 0.62f)

    val shimmer = if (style.liquid && !reduceMotion) {
        val transition = rememberInfiniteTransition(label = "Liquid glass sheen")
        val value by transition.animateFloat(
            initialValue = 0.22f,
            targetValue = 0.64f,
            animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
            label = "Liquid glass highlight"
        )
        value
    } else 0.34f

    Surface(
        modifier = modifier,
        shape = shape,
        color = fill,
        border = BorderStroke(if (style.liquid) 1.25.dp else 1.dp, border),
        tonalElevation = 0.dp,
        shadowElevation = if (style.liquid) 5.dp else 2.dp
    ) {
        val highlight = if (dark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.72f)
        Column(
            Modifier.clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            highlight.copy(alpha = highlight.alpha * shimmer),
                            Color.Transparent,
                            colors.primary.copy(alpha = if (style.liquid) 0.075f else 0.035f),
                            Color.Transparent
                        ),
                        start = Offset.Zero,
                        end = Offset(900f, 900f)
                    )
                )
                .padding(if (compact) 14.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

/** Real Android window-background blur for supported devices. */
fun applyNurWindowBlur(window: Window, enabled: Boolean, radius: Int = 32) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { blurBehindRadius = radius.coerceIn(0, 80) }
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { blurBehindRadius = 0 }
        }
    }
}
