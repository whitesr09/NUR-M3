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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
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

/** True when a main destination is intentionally drawing underneath the fixed glass chrome. */
val LocalNurUnderChrome = staticCompositionLocalOf { false }

/**
 * Main destinations keep their first/last items clear of the floating chrome while the LazyColumn
 * itself remains full-screen. Once the user scrolls, content can visibly travel behind the glass.
 */
@Composable
fun NurScrollContentPadding(home: Boolean = false, horizontal: Dp = NurDesign.pagePadding): PaddingValues {
    return if (LocalNurUnderChrome.current) {
        PaddingValues(
            start = horizontal,
            end = horizontal,
            top = if (home) 148.dp else 108.dp,
            bottom = 116.dp
        )
    } else PaddingValues(horizontal)
}

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
            Canvas(Modifier.matchParentSize().blur((style.blurRadius.coerceAtLeast(12)).dp)) {
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

/**
 * Stronger glass used by fixed top/bottom chrome. The scrolling content is drawn underneath it,
 * so translucency is genuine rather than a solid card painted to look transparent.
 */
@Composable
fun NurGlassChromeSurface(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(30.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val style = LocalNurGlass.current
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.35f
    val enabled = style.enabled
    val alpha = if (!enabled) 0.96f else when (style.mode) {
        "subtle" -> 0.78f
        "frosted" -> 0.53f
        else -> 0.39f
    }.let { (it * (0.72f + style.intensity * 0.28f)).coerceIn(0.30f, 0.92f) }
    val borderColor = if (!enabled) scheme.outlineVariant.copy(alpha = 0.72f)
        else if (dark) Color.White.copy(alpha = if (style.liquid) 0.24f else 0.16f)
        else Color.White.copy(alpha = if (style.liquid) 0.80f else 0.64f)

    Surface(
        modifier = modifier,
        shape = shape,
        color = scheme.surface.copy(alpha = alpha),
        border = BorderStroke(if (style.liquid) 1.25.dp else 1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = if (enabled) 10.dp else 2.dp
    ) {
        val glow = if (enabled) scheme.primary.copy(alpha = if (style.liquid) 0.11f else 0.055f) else Color.Transparent
        Box(
            Modifier.fillMaxWidth().clip(shape).background(
                Brush.linearGradient(
                    listOf(
                        if (dark) Color.White.copy(alpha = if (enabled) 0.075f else 0f) else Color.White.copy(alpha = if (enabled) 0.35f else 0f),
                        Color.Transparent,
                        glow,
                        Color.Transparent
                    ),
                    start = Offset.Zero,
                    end = Offset(1000f, 700f)
                )
            ),
            content = content
        )
    }
}

/** Circular glass control used for NUR AI and other floating chrome actions. */
@Composable
fun NurGlassFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Action",
    content: @Composable BoxScope.() -> Unit
) {
    val style = LocalNurGlass.current
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.35f
    val alpha = if (!style.enabled) 0.96f else if (style.liquid) 0.48f else 0.60f
    val border = if (dark) Color.White.copy(alpha = if (style.enabled) 0.24f else 0.10f)
        else Color.White.copy(alpha = if (style.enabled) 0.82f else 0.25f)
    Surface(
        onClick = onClick,
        modifier = modifier.size(64.dp),
        shape = CircleShape,
        color = scheme.primaryContainer.copy(alpha = alpha),
        contentColor = scheme.onPrimaryContainer,
        border = BorderStroke(1.25.dp, border),
        shadowElevation = if (style.enabled) 12.dp else 6.dp
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = if (style.enabled) 0.13f else 0f), Color.Transparent, scheme.primary.copy(alpha = 0.12f))
                )
            ),
            contentAlignment = Alignment.Center
        ) { content() }
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
