package com.nshd.nurm3.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/** Glass is an optional visual treatment; it never changes progress or user data. */
data class NurGlassStyle(val mode: String = "off", val intensity: Float = 0.55f, val radius: Int = 20) {
    val enabled get() = mode != "off"
    companion object {
        fun normalize(mode: String?, intensity: Float, radius: Int) = NurGlassStyle(
            mode?.takeIf { it in setOf("off", "subtle", "frosted", "liquid") } ?: "off",
            if (intensity.isFinite()) intensity.coerceIn(0.15f, 0.85f) else 0.55f,
            radius.coerceIn(8, 32))
    }
}
val LocalNurGlass = staticCompositionLocalOf { NurGlassStyle() }

/** Translucent, layered glass. Does not claim to blur a live backdrop or private content. */
@Composable
fun NurGlassSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val style = LocalNurGlass.current
    val colors = MaterialTheme.colorScheme
    val compact = LocalNurCompact.current
    val reduceMotion = LocalNurReduceMotion.current
    val shape = if (style.enabled) RoundedCornerShape(style.radius.dp) else MaterialTheme.shapes.large
    val dark = colors.background.luminance() < 0.3f
    val enabled = style.enabled
    val isAmoled = colors.background == Color.Black && colors.surface == Color.Black
    val effectiveMode = if (isAmoled && style.mode == "liquid") "subtle" else style.mode
    val fill = if (!enabled) colors.surface else {
        val base = if (dark) colors.surfaceContainer else colors.surfaceContainerLow
        val opacity = when (effectiveMode) {
            "subtle" -> 0.94f
            "frosted" -> 0.88f
            else -> 0.82f
        }
        base.copy(alpha = (opacity + (1f - style.intensity) * 0.06f).coerceAtMost(1f))
    }
    val outline = colors.outlineVariant.copy(alpha = if (enabled) 0.8f else 0.65f)
    Surface(modifier = modifier, shape = shape, color = fill, border = BorderStroke(1.dp, outline), tonalElevation = 0.dp, shadowElevation = 0.dp) {
        val highlight = if (dark) Color.White.copy(alpha = 0.045f) else Color.White.copy(alpha = 0.45f)
        val layer = if (enabled && !reduceMotion) Brush.verticalGradient(listOf(highlight, Color.Transparent, colors.primary.copy(alpha = 0.018f))) else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
        Column(Modifier.clip(shape).background(layer).padding(if (compact) 14.dp else 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
