package com.nshd.nurm3.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.nshd.nurm3.ui.NurGlassStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.nurAdvancedSettings by preferencesDataStore(name = "nur_visual_v15")

/** A separate DataStore avoids introducing a second owner of the legacy settings file. */
class AdvancedSettings(private val context: Context) {
    private val glassMode = stringPreferencesKey("glass_mode")
    private val glassIntensity = floatPreferencesKey("glass_intensity")
    private val glassRadius = intPreferencesKey("glass_radius")
    private val ringStyle = stringPreferencesKey("ring_style")
    private val barStyle = stringPreferencesKey("bar_style")
    private val ringStroke = floatPreferencesKey("ring_stroke")
    private val barStroke = floatPreferencesKey("bar_stroke")
    private val motion = floatPreferencesKey("motion_intensity")

    val preferences: Flow<VisualPreferences> = context.nurAdvancedSettings.data.map { p ->
        VisualPreferences.normalize(p[glassMode], p[glassIntensity] ?: 0.55f, p[glassRadius] ?: 20,
            p[ringStyle], p[barStyle], p[ringStroke] ?: 1f, p[barStroke] ?: 1f, p[motion] ?: 1f)
    }

    suspend fun updateChoice(key: String, value: String) {
        context.nurAdvancedSettings.edit { p ->
            when (key) {
                "glass_mode" -> if (value in setOf("off", "subtle", "frosted", "liquid")) p[glassMode] = value
                "ring_style" -> if (value in AppearanceChoices.progressStyles) p[ringStyle] = value
                "bar_style" -> if (value in AppearanceChoices.progressStyles) p[barStyle] = value
            }
        }
    }

    suspend fun updateFloat(key: String, value: Float) {
        if (!value.isFinite()) return
        context.nurAdvancedSettings.edit { p ->
            when (key) {
                "glass_intensity" -> p[glassIntensity] = value.coerceIn(0.15f, 0.85f)
                "ring_stroke" -> p[ringStroke] = value.coerceIn(0.6f, 1.8f)
                "bar_stroke" -> p[barStroke] = value.coerceIn(0.6f, 1.8f)
                "motion_intensity" -> p[motion] = value.coerceIn(0f, 1.5f)
            }
        }
    }

    suspend fun updateRadius(value: Int) {
        context.nurAdvancedSettings.edit { it[glassRadius] = value.coerceIn(8, 32) }
    }

    suspend fun reset() {
        context.nurAdvancedSettings.edit { it.clear() }
    }
}
