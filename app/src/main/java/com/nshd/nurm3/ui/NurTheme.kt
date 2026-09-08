package com.nshd.nurm3.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.data.NurPreferences

private val Night = Color(0xFF101820)
private val NightSurface = Color(0xFF19242E)
private val Gold = Color(0xFFD9B96F)
private val Light = Color(0xFFFAF8F2)

val LocalNurCompact = staticCompositionLocalOf { false }

private fun paletteColor(name: String, dark: Boolean): Color = when (name) {
    "ocean" -> if (dark) Color(0xFFA9CEE8) else Color(0xFF315D78)
    "sage" -> if (dark) Color(0xFFADD1B9) else Color(0xFF3C6751)
    "rose" -> if (dark) Color(0xFFE7B9C6) else Color(0xFF8B465D)
    else -> if (dark) Gold else Color(0xFF806017)
}

private fun typography(scale: Float): Typography {
    val base = Typography()
    fun TextStyle.scaled() = copy(fontSize = fontSize * scale, lineHeight = lineHeight * scale)
    return Typography(
        displayLarge = base.displayLarge.scaled(), displayMedium = base.displayMedium.scaled(), displaySmall = base.displaySmall.scaled(),
        headlineLarge = base.headlineLarge.scaled(), headlineMedium = base.headlineMedium.scaled(), headlineSmall = base.headlineSmall.scaled(),
        titleLarge = base.titleLarge.scaled(), titleMedium = base.titleMedium.scaled(), titleSmall = base.titleSmall.scaled(),
        bodyLarge = base.bodyLarge.scaled(), bodyMedium = base.bodyMedium.scaled(), bodySmall = base.bodySmall.scaled(),
        labelLarge = base.labelLarge.scaled(), labelMedium = base.labelMedium.scaled(), labelSmall = base.labelSmall.scaled()
    )
}

@Composable
fun NurTheme(prefs: NurPreferences, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = when (prefs.themeMode) {
        "light" -> false
        "system" -> isSystemInDarkTheme()
        else -> true
    }
    val primary = paletteColor(prefs.palette, dark)
    val scheme = when {
        prefs.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme(
            primary = primary, onPrimary = Night, primaryContainer = Color(0xFF384550), onPrimaryContainer = Color(0xFFF2DEB0),
            secondary = Color(0xFFB8C8CF), onSecondary = Night, secondaryContainer = Color(0xFF30414C), onSecondaryContainer = Color(0xFFD5E3E8),
            tertiary = Color(0xFFB8CBBF), onTertiary = Night, background = Night, onBackground = Color(0xFFF0EEE8),
            surface = NightSurface, onSurface = Color(0xFFF0EEE8), surfaceVariant = Color(0xFF26343E), onSurfaceVariant = Color(0xFFBAC6CC),
            outline = Color(0xFF77858D), outlineVariant = Color(0xFF414D55), surfaceTint = primary
        )
        else -> lightColorScheme(
            primary = primary, onPrimary = Color.White, primaryContainer = Color(0xFFF4E3B8), onPrimaryContainer = Color(0xFF332809),
            secondary = Color(0xFF6B6049), onSecondary = Color.White, secondaryContainer = Color(0xFFECE3D2), onSecondaryContainer = Color(0xFF262014),
            tertiary = Color(0xFF52695A), onTertiary = Color.White, background = Light, onBackground = Color(0xFF202B31),
            surface = Color(0xFFFFFDF7), onSurface = Color(0xFF202B31), surfaceVariant = Color(0xFFECE8DC), onSurfaceVariant = Color(0xFF5A6062),
            outline = Color(0xFF777A78), outlineVariant = Color(0xFFD0D1CC), surfaceTint = primary
        )
    }
    val shapes = if (prefs.softShapes) Shapes(
        small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
    ) else Shapes(
        small = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    )
    CompositionLocalProvider(LocalNurCompact provides prefs.compactCards) {
        MaterialTheme(colorScheme = scheme, typography = typography(prefs.typeScale), shapes = shapes, content = content)
    }
}
