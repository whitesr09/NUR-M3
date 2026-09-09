package com.nshd.nurm3.ui

import android.os.Build
import android.view.Window
import android.view.WindowManager
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

/** Glass is optional and never changes the underlying activity or contrast semantics. */
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

@Composable
fun NurGlassSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val style = LocalNurGlass.current
    val colors = MaterialTheme.colorScheme
    val compact = LocalNurCompact.current
    val shape = RoundedCornerShape(style.radius.dp)
    if (!style.enabled) {
        Surface(modifier, shape = MaterialTheme.shapes.large, color = colors.surface,
            border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.65f))) {
            Column(Modifier.padding(if (compact) 14.dp else 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    } else {
        val dark = colors.background.luminance() < 0.3f
        val alpha = when (style.mode) { "subtle" -> 0.86f; "frosted" -> 0.68f; else -> 0.54f } * style.intensity + 0.15f
        val fill = colors.surface.copy(alpha = alpha.coerceIn(0.3f, 0.95f))
        val highlight = if (dark) Color.White.copy(alpha = 0.13f) else Color.White.copy(alpha = 0.72f)
        Surface(modifier, shape = shape, color = fill,
            border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.65f))) {
            Column(Modifier.clip(shape).background(Brush.verticalGradient(listOf(highlight.copy(alpha = highlight.alpha * 0.35f), Color.Transparent, colors.primary.copy(alpha = 0.025f))))
                .padding(if (compact) 14.dp else 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

/** Real window-background blur is only enabled where Android supports it. Ordinary cards use a translucent glass treatment, not a fabricated backdrop screenshot. */
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
