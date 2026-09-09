package com.nshd.nurm3.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.nshd.nurm3.data.VisualPreferences

val LocalNurRingStyle = staticCompositionLocalOf { "slim" }
val LocalNurBarStyle = staticCompositionLocalOf { "slim" }
val LocalNurRingStroke = staticCompositionLocalOf { 1f }
val LocalNurBarStroke = staticCompositionLocalOf { 1f }
val LocalNurMotionIntensity = staticCompositionLocalOf { 1f }

@Composable
fun NurAdvancedMotion(visual: VisualPreferences, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalNurRingStyle provides visual.ringStyle,
        LocalNurBarStyle provides visual.barStyle,
        LocalNurRingStroke provides visual.ringStroke,
        LocalNurBarStroke provides visual.barStroke,
        LocalNurMotionIntensity provides visual.motionIntensity,
        content = content
    )
}
