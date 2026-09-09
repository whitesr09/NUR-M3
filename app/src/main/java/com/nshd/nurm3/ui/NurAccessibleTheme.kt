package com.nshd.nurm3.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.nshd.nurm3.data.NurAccessibility
import com.nshd.nurm3.data.NurPreferences

@Composable
fun NurAccessibleTheme(prefs: NurPreferences, accessibility: NurAccessibility, content: @Composable () -> Unit) {
    NurTheme(prefs) {
        val dark = when (prefs.themeMode) {
            "light" -> false
            "system" -> isSystemInDarkTheme()
            else -> true
        }
        val base = MaterialTheme.colorScheme
        val scheme = if (!accessibility.highContrast) base else if (dark) base.copy(
            background = Color.Black, surface = Color.Black, surfaceVariant = Color(0xFF111111),
            surfaceContainer = Color.Black, surfaceContainerLow = Color.Black, surfaceContainerHigh = Color(0xFF101010),
            onBackground = Color.White, onSurface = Color.White, onSurfaceVariant = Color(0xFFE5E5E5),
            outline = Color(0xFFD0D0D0), outlineVariant = Color(0xFF888888),
            primary = Color(0xFFFFD67A), onPrimary = Color.Black, surfaceTint = Color.Transparent
        ) else base.copy(
            background = Color.White, surface = Color.White, surfaceVariant = Color(0xFFF4F4F4),
            surfaceContainer = Color.White, surfaceContainerLow = Color.White, surfaceContainerHigh = Color(0xFFE8E8E8),
            onBackground = Color.Black, onSurface = Color.Black, onSurfaceVariant = Color(0xFF252525),
            outline = Color(0xFF454545), outlineVariant = Color(0xFF777777),
            primary = Color(0xFF654A0B), onPrimary = Color.White, surfaceTint = Color.Transparent
        )
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
