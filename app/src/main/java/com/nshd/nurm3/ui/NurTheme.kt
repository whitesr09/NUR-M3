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

private val Night = Color(0xFF0C100D)
private val NightSurface = Color(0xFF131814)
private val Light = Color(0xFFFAF7F1)
val LocalNurCompact = staticCompositionLocalOf { false }
val LocalNurProgressStyle = staticCompositionLocalOf { "slim" }
private fun paletteColor(name: String, dark: Boolean): Color = when (name) {
    "ocean" -> if (dark) Color(0xFFA9CEE8) else Color(0xFF315D78)
    "sage" -> if (dark) Color(0xFFADD1B9) else Color(0xFF3C6751)
    "rose" -> if (dark) Color(0xFFE7B9C6) else Color(0xFF8B465D)
    "emerald" -> if (dark) Color(0xFF8FD8B2) else Color(0xFF1E6B45)
    "violet" -> if (dark) Color(0xFFCDB4F6) else Color(0xFF5E3FA8)
    else -> if (dark) NurDesign.gold else Color(0xFF806017)
}
/** Container pair (container, onContainer) tuned per palette so non-gold accents don't sit on gold-tinted surfaces. */
private fun paletteContainer(name: String, dark: Boolean): Pair<Color, Color> = when (name) {
    "ocean" -> if (dark) Color(0xFF1D3547) to Color(0xFFCFE7F5) else Color(0xFFD8E9F2) to Color(0xFF17384E)
    "sage" -> if (dark) Color(0xFF24382B) to Color(0xFFD3EDDB) else Color(0xFFDCEEE1) to Color(0xFF274434)
    "rose" -> if (dark) Color(0xFF422630) to Color(0xFFF6DCE4) else Color(0xFFF7E0E7) to Color(0xFF572836)
    "emerald" -> if (dark) Color(0xFF14402A) to Color(0xFFC9F2DA) else Color(0xFFCFF0DC) to Color(0xFF0E4630)
    "violet" -> if (dark) Color(0xFF35245C) to Color(0xFFE7DCFC) else Color(0xFFE6DBFA) to Color(0xFF38216B)
    else -> if (dark) Color(0xFF40341F) to Color(0xFFF7E5B7) else Color(0xFFF3E8CC) to Color(0xFF4B370B)
}
private fun typography(scale: Float, family: FontFamily): Typography {
    val base = Typography()
    fun TextStyle.scaled() = copy(fontSize = fontSize * scale, lineHeight = lineHeight * scale, fontFamily = family)
    fun TextStyle.relaxed() = copy(lineHeight = lineHeight * 1.08f)
    return Typography(
        displayLarge = base.displayLarge.scaled(), displayMedium = base.displayMedium.scaled(), displaySmall = base.displaySmall.scaled(),
        headlineLarge = base.headlineLarge.scaled().copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.scaled().copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.scaled().copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.scaled().copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.scaled().copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.scaled().copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.scaled().relaxed(), bodyMedium = base.bodyMedium.scaled().relaxed(), bodySmall = base.bodySmall.scaled().relaxed(),
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
@Composable
fun NurTheme(prefs: NurPreferences, content: @Composable () -> Unit) {
    val context = LocalContext.current
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
    val (primaryContainer, onPrimaryContainer) = paletteContainer(prefs.palette, dark)
    val scheme = when {
        prefs.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val base = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (prefs.themeMode == "amoled") amoledScheme(base) else base
        }
        dark -> {
            val base = darkColorScheme(
                primary = primary, onPrimary = Night, primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
                secondary = Color(0xFFB9C6BB), onSecondary = Night, secondaryContainer = Color(0xFF26312A), onSecondaryContainer = Color(0xFFDCE7DC),
                tertiary = Color(0xFFB8CBBF), onTertiary = Night,
                background = Night, onBackground = Color(0xFFE9EDE4), surface = NightSurface, onSurface = Color(0xFFE9EDE4),
                surfaceVariant = Color(0xFF2A332B), onSurfaceVariant = Color(0xFFB9C4B4),
                surfaceContainerLowest = Night, surfaceContainerLow = Color(0xFF171D18), surfaceContainer = Color(0xFF1C241D),
                surfaceContainerHigh = Color(0xFF232C24), surfaceContainerHighest = Color(0xFF2B362C),
                outline = Color(0xFF8A968A), outlineVariant = Color(0xFF3A453B), surfaceTint = primary
            )
            if (prefs.themeMode == "amoled") amoledScheme(base) else base
        }
        else -> lightColorScheme(
            primary = primary, onPrimary = Color.White, primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
            secondary = Color(0xFF5A665C), onSecondary = Color.White, secondaryContainer = Color(0xFFE4E9DF), onSecondaryContainer = Color(0xFF2A332A),
            tertiary = Color(0xFF52695A), onTertiary = Color.White,
            background = Light, onBackground = Color(0xFF22271F), surface = Color(0xFFFFFDF8), onSurface = Color(0xFF22271F),
            surfaceVariant = Color(0xFFEAE5D8), onSurfaceVariant = Color(0xFF5C6656),
            surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF5F1E8), surfaceContainer = Color(0xFFEFEBE0),
            surfaceContainerHigh = Color(0xFFE9E3D5), surfaceContainerHighest = Color(0xFFE1DACA),
            outline = Color(0xFF7A8272), outlineVariant = Color(0xFFDAD3C2), surfaceTint = primary
        )
    }
    val shapes = if (prefs.softShapes) Shapes(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
        small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
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
