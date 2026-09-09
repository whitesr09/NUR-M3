package com.nshd.nurm3.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nshd.nurm3.data.NurFontStore
import com.nshd.nurm3.data.NurPreferences

private val Night = Color(0xFF0B131B)
private val NightSurface = Color(0xFF111C25)
private val Light = Color(0xFFF7F7F4)
val LocalNurCompact = staticCompositionLocalOf { false }
val LocalNurProgressStyle = staticCompositionLocalOf { "slim" }
private fun paletteColor(name: String, dark: Boolean): Color = when (name) {
    "ocean" -> if (dark) Color(0xFFA9CEE8) else Color(0xFF315D78)
    "sage" -> if (dark) Color(0xFFADD1B9) else Color(0xFF3C6751)
    "rose" -> if (dark) Color(0xFFE7B9C6) else Color(0xFF8B465D)
    else -> if (dark) NurDesign.gold else Color(0xFF806017)
}
private fun typography(scale: Float, family: FontFamily): Typography {
    val base = Typography()
    fun TextStyle.scaled() = copy(fontSize = fontSize * scale, lineHeight = lineHeight * scale, fontFamily = family)
    return Typography(
        displayLarge = base.displayLarge.scaled(), displayMedium = base.displayMedium.scaled(), displaySmall = base.displaySmall.scaled(),
        headlineLarge = base.headlineLarge.scaled().copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.scaled().copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.scaled().copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.scaled().copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.scaled().copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.scaled().copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.scaled(), bodyMedium = base.bodyMedium.scaled(), bodySmall = base.bodySmall.scaled(),
        labelLarge = base.labelLarge.scaled().copy(fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.scaled(),
        labelSmall = base.labelSmall.scaled().copy(letterSpacing = 0.3.sp)
    )
}
private fun amoledScheme(base: ColorScheme): ColorScheme = base.copy(
    background = Color.Black, surface = Color.Black, surfaceTint = Color.Black,
    surfaceContainerLowest = Color.Black, surfaceContainerLow = Color.Black,
    surfaceContainer = Color.Black, surfaceContainerHigh = Color(0xFF101010),
    surfaceContainerHighest = Color(0xFF181818), surfaceVariant = Color(0xFF191919),
    onBackground = Color(0xFFE8EDF0), onSurface = Color(0xFFE8EDF0),
    onSurfaceVariant = Color(0xFFB1BEC6), outlineVariant = Color(0xFF383838)
)

private fun glassScheme(base: ColorScheme, glass: NurGlassStyle): ColorScheme {
    if (!glass.enabled) return base
    val surfaceAlpha = when (glass.mode) {
        "subtle" -> 0.92f
        "frosted" -> 0.78f
        else -> 0.68f
    }
    val variantAlpha = (surfaceAlpha - 0.08f).coerceAtLeast(0.55f)
    return base.copy(
        surface = base.surface.copy(alpha = surfaceAlpha),
        surfaceVariant = base.surfaceVariant.copy(alpha = variantAlpha),
        surfaceContainerLowest = base.surfaceContainerLowest.copy(alpha = surfaceAlpha),
        surfaceContainerLow = base.surfaceContainerLow.copy(alpha = variantAlpha),
        surfaceContainer = base.surfaceContainer.copy(alpha = variantAlpha),
        surfaceContainerHigh = base.surfaceContainerHigh.copy(alpha = variantAlpha),
        surfaceContainerHighest = base.surfaceContainerHighest.copy(alpha = variantAlpha),
        primaryContainer = base.primaryContainer.copy(alpha = if (glass.liquid) 0.82f else 0.90f),
        secondaryContainer = base.secondaryContainer.copy(alpha = if (glass.liquid) 0.78f else 0.88f)
    )
}

@Composable
fun NurTheme(prefs: NurPreferences, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val glass = LocalNurGlass.current
    val dark = when (prefs.themeMode) {
        "light" -> false
        "system" -> isSystemInDarkTheme()
        else -> true
    }
    val family = remember(context, prefs.fontStyle, prefs.customFontId) {
        when (prefs.fontStyle) {
            "sans" -> FontFamily.SansSerif
            "serif" -> FontFamily.Serif
            "mono" -> FontFamily.Monospace
            "rounded" -> FontFamily.Cursive
            "custom" -> {
                val file = NurFontStore.file(context, prefs.customFontId)
                if (file != null && file.isFile) runCatching { FontFamily(Font(file)) }.getOrDefault(FontFamily.SansSerif)
                else FontFamily.SansSerif
            }
            else -> FontFamily.Default
        }
    }
    val primary = paletteColor(prefs.palette, dark)
    val baseScheme = when {
        prefs.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val base = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (prefs.themeMode == "amoled") amoledScheme(base) else base
        }
        dark -> {
            val base = darkColorScheme(
                primary = primary, onPrimary = Night, primaryContainer = Color(0xFF40341F), onPrimaryContainer = Color(0xFFF7E5B7),
                secondary = Color(0xFFB5C8D2), onSecondary = Night, secondaryContainer = Color(0xFF263844), onSecondaryContainer = Color(0xFFD9E5EB),
                tertiary = Color(0xFFB8CBBF), onTertiary = Night,
                background = Night, onBackground = Color(0xFFE8EDF0), surface = NightSurface, onSurface = Color(0xFFE8EDF0),
                surfaceVariant = Color(0xFF263640), onSurfaceVariant = Color(0xFFB1BEC6),
                surfaceContainerLowest = Night, surfaceContainerLow = Color(0xFF15212A), surfaceContainer = Color(0xFF1B2832),
                surfaceContainerHigh = Color(0xFF23323C), surfaceContainerHighest = Color(0xFF2B3B46),
                outline = Color(0xFF81909A), outlineVariant = Color(0xFF354650), surfaceTint = primary
            )
            if (prefs.themeMode == "amoled") amoledScheme(base) else base
        }
        else -> lightColorScheme(
            primary = primary, onPrimary = Color.White, primaryContainer = Color(0xFFF3E8CC), onPrimaryContainer = Color(0xFF4B370B),
            secondary = Color(0xFF57666E), onSecondary = Color.White, secondaryContainer = Color(0xFFE3EAED), onSecondaryContainer = Color(0xFF283942),
            tertiary = Color(0xFF52695A), onTertiary = Color.White,
            background = Light, onBackground = Color(0xFF202B31), surface = Color(0xFFFDFDFA), onSurface = Color(0xFF202B31),
            surfaceVariant = Color(0xFFE9EDEB), onSurfaceVariant = Color(0xFF56646A),
            surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF4F5F1), surfaceContainer = Color(0xFFEDF0EC),
            surfaceContainerHigh = Color(0xFFE6EAE6), surfaceContainerHighest = Color(0xFFDEE4DF),
            outline = Color(0xFF75838A), outlineVariant = Color(0xFFD5DDDA), surfaceTint = primary
        )
    }
    val scheme = glassScheme(baseScheme, glass)
    val shapes = if (prefs.softShapes) Shapes(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
        small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    ) else Shapes(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(2.dp),
        small = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(7.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    )
    CompositionLocalProvider(
        LocalNurCompact provides prefs.compactCards,
        LocalNurReduceMotion provides prefs.reduceMotion,
        LocalNurProgressStyle provides prefs.progressStyle
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography(prefs.typeScale, family), shapes = shapes, content = content)
    }
}
