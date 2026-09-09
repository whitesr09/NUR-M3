package com.nshd.nurm3.ui

import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/** Glass is optional and never changes the underlying activity or accessibility semantics. */
data class NurGlassStyle(val mode: String = "off", val intensity: Float = 0.55f, val radius: Int = 20) {
    val enabled get() = mode != "off"
    val liquid get() = mode == "liquid"
    companion object {
        fun normalize(mode: String?, intensity: Float, radius: Int) = NurGlassStyle(
            mode?.takeIf { it in setOf("off", "subtle", "frosted", "liquid") } ?: "off",
            if (intensity.isFinite()) intensity.coerceIn(0.15f, 0.85f) else 0.55f,
            radius.coerceIn(8, 32)
        )
    }
}
val LocalNurGlass = staticCompositionLocalOf { NurGlassStyle() }

/**
 * Decorative backdrop owned by NUR itself. Blurring these layers makes translucent panels
 * visibly read as glass even on OEMs that disable system window blur.
 */
@Composable
fun NurGlassBackdrop(modifier: Modifier = Modifier) {
    val style = LocalNurGlass.current
    val scheme = MaterialTheme.colorScheme
    Box(modifier.background(scheme.background)) {
        if (style.enabled) {
            val strength = style.intensity
            val blur = when (style.mode) {
                "subtle" -> 54.dp
                "frosted" -> 72.dp
                else -> 88.dp
            }
            Box(
                Modifier.align(Alignment.TopEnd)
                    .offset(x = 72.dp, y = (-80).dp)
                    .size(if (style.liquid) 320.dp else 270.dp)
                    .blur(blur)
                    .background(scheme.primary.copy(alpha = 0.16f + strength * 0.12f), CircleShape)
            )
            Box(
                Modifier.align(Alignment.BottomStart)
                    .offset(x = (-85).dp, y = 72.dp)
                    .size(if (style.liquid) 360.dp else 300.dp)
                    .blur(blur)
                    .background(scheme.tertiary.copy(alpha = 0.10f + strength * 0.10f), CircleShape)
            )
            if (style.liquid) {
                Box(
                    Modifier.align(Alignment.Center)
                        .offset(x = 115.dp, y = 40.dp)
                        .size(220.dp)
                        .blur(96.dp)
                        .background(scheme.secondary.copy(alpha = 0.11f + strength * 0.08f), CircleShape)
                )
            }
        }
    }
}

@Composable
fun NurGlassSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val style = LocalNurGlass.current
    val colors = MaterialTheme.colorScheme
    val compact = LocalNurCompact.current
    val shape = RoundedCornerShape(style.radius.dp)
    if (!style.enabled) {
        Surface(
            modifier,
            shape = MaterialTheme.shapes.large,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.65f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(Modifier.padding(if (compact) 14.dp else 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    } else {
        val dark = colors.background.luminance() < 0.3f
        val baseAlpha = when (style.mode) {
            "subtle" -> 0.78f
            "frosted" -> 0.54f
            else -> 0.40f
        }
        val alpha = (baseAlpha - (style.intensity - 0.15f) * 0.22f).coerceIn(0.26f, 0.84f)
        val fill = colors.surface.copy(alpha = alpha)
        val highlight = if (dark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.82f)
        val edge = if (dark) Color.White.copy(alpha = 0.13f + style.intensity * 0.12f)
        else colors.outlineVariant.copy(alpha = 0.72f)
        val shadow = if (style.liquid) 12.dp else 6.dp
        Surface(
            modifier = modifier.shadow(shadow, shape, clip = false),
            shape = shape,
            color = fill,
            border = BorderStroke(if (style.liquid) 1.2.dp else 1.dp, edge),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                Modifier.clip(shape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                highlight.copy(alpha = highlight.alpha * (0.35f + style.intensity * 0.25f)),
                                Color.Transparent,
                                colors.primary.copy(alpha = if (style.liquid) 0.075f else 0.035f)
                            )
                        )
                    )
                    .padding(if (compact) 14.dp else 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

/** Real system blur is requested only on Android versions that expose it. */
fun applyNurWindowBlur(window: Window, enabled: Boolean, radius: Int = 24) {
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
