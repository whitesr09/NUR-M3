package com.nshd.nurm3.ui

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import android.graphics.RenderEffect
import com.nshd.nurm3.data.NurPreferences
import com.nshd.nurm3.data.VisualPreferences

val LocalNurVisual = staticCompositionLocalOf { VisualPreferences() }

/** Preserves the existing font, palette, AMOLED and dynamic-color implementation. */
@Composable
fun NurTheme15(prefs: NurPreferences, visual: VisualPreferences, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNurVisual provides visual, LocalNurGlass provides visual.glass) {
        NurTheme(prefs, content)
    }
}

/** Only app-owned decorative pixels are blurred. No screenshot or private content is sampled. */
@Composable
fun NurGlassBackdrop(modifier: Modifier = Modifier) {
    val visual = LocalNurVisual.current
    val colors = MaterialTheme.colorScheme
    val disabled = !visual.glass.enabled || colors.background == Color.Black || LocalNurReduceMotion.current
    Box(modifier.background(colors.background)) {
        if (!disabled) {
            val blur = if (Build.VERSION.SDK_INT >= 31) Modifier.graphicsLayer {
                renderEffect = RenderEffect.createBlurEffect(36f, 36f, android.graphics.Shader.TileMode.CLAMP).asComposeRenderEffect()
            } else Modifier
            Canvas(Modifier.fillMaxSize().then(blur)) {
                val w = size.width
                val h = size.height
                drawRect(brush = Brush.verticalGradient(listOf(colors.background, colors.surfaceContainerLow, colors.background)))
                drawCircle(colors.primary.copy(alpha = 0.10f), radius = w * 0.65f, center = Offset(w * 0.08f, h * 0.18f))
                drawCircle(colors.tertiary.copy(alpha = 0.09f), radius = w * 0.7f, center = Offset(w * 0.95f, h * 0.63f))
            }
        }
    }
}
