package com.nshd.nurm3.ui

import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Compatibility holder for older saved appearance preferences.
 * Glass rendering is intentionally disabled for this release; the values are kept so a future
 * properly-designed glass system can be reintroduced without corrupting existing preferences.
 */
data class NurGlassStyle(
    val mode: String = "off",
    val intensity: Float = 0f,
    val blurRadius: Int = 0
) {
    val enabled get() = false
    val liquid get() = false
    val cornerRadius get() = 20

    companion object {
        fun normalize(mode: String?, intensity: Float, blurRadius: Int) = NurGlassStyle()
    }
}

val LocalNurGlass = staticCompositionLocalOf { NurGlassStyle() }
val LocalNurUnderChrome = staticCompositionLocalOf { false }

/** Keep content clear of fixed top/bottom chrome while using fully opaque surfaces. */
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

/** Root surface: opaque and explicitly supplies the correct theme content color. */
@Composable
fun NurGlassBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(Modifier.fillMaxSize(), content = content)
    }
}

/** Standard solid NUR card surface. */
@Composable
fun NurGlassSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    val compact = LocalNurCompact.current
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = colors.surface,
        contentColor = colors.onSurface,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.65f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            Modifier.padding(if (compact) 14.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

/** Fixed chrome is now fully opaque. No blur, translucency, sheen or backdrop visibility. */
@Composable
fun NurGlassChromeSurface(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(30.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = shape,
        color = scheme.surface,
        contentColor = scheme.onSurface,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.72f)),
        tonalElevation = 0.dp,
        shadowElevation = 2.dp
    ) {
        Box(Modifier.fillMaxWidth(), content = content)
    }
}

/** NUR AI floating action button using normal Material surfaces. */
@Composable
fun NurGlassFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Action",
    content: @Composable BoxScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.size(60.dp),
        shape = CircleShape,
        color = scheme.primaryContainer,
        contentColor = scheme.onPrimaryContainer,
        shadowElevation = 6.dp
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
    }
}

/** Explicitly remove any blur flag left by an older preference/build. */
fun applyNurWindowBlur(window: Window, enabled: Boolean, radius: Int = 0) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        window.attributes = window.attributes.apply { blurBehindRadius = 0 }
    }
}
