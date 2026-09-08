package com.nshd.nurm3.ui

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.nshd.nurm3.data.NurPreferences

private val Gold = Color(0xFFD9B96F)
private val Night = Color(0xFF101820)
private val NightSurface = Color(0xFF19242E)

@Composable
fun NurTheme(prefs: NurPreferences, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scheme = when {
        prefs.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (prefs.darkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        prefs.darkMode -> darkColorScheme(
            primary = if (prefs.goldAccent) Gold else Color(0xFFAFC8DD),
            onPrimary = Night, secondary = Color(0xFFB8C8CF), background = Night,
            surface = NightSurface, onSurface = Color(0xFFF0EEE8),
            surfaceVariant = Color(0xFF26343E), onSurfaceVariant = Color(0xFFBAC6CC))
        else -> lightColorScheme(
            primary = if (prefs.goldAccent) Color(0xFF806017) else Color(0xFF345E78),
            onPrimary = Color.White, secondary = Color(0xFF6B6049),
            background = Color(0xFFFAF8F2), surface = Color(0xFFFFFDF7),
            surfaceVariant = Color(0xFFECE8DC), onSurface = Color(0xFF202B31))
    }
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}
