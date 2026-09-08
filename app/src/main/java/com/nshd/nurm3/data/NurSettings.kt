package com.nshd.nurm3.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.nshd.nurm3.ui.JourneyLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.nurDataStore by preferencesDataStore(name = "nur_settings")

data class NurPreferences(
    val darkMode: Boolean = true,
    val dynamicColor: Boolean = false,
    val goldAccent: Boolean = true,
    val reduceMotion: Boolean = false,
    val showArabic: Boolean = true,
    val themeMode: String = "dark",
    val palette: String = "gold",
    val typeScale: Float = 1f,
    val compactCards: Boolean = false,
    val softShapes: Boolean = true,
    val journey: JourneyLayout = JourneyLayout.DEFAULT
)

class NurSettings(private val context: Context) {
    private val dark = booleanPreferencesKey("dark_mode")
    private val dynamic = booleanPreferencesKey("dynamic_color")
    private val gold = booleanPreferencesKey("gold_accent")
    private val motion = booleanPreferencesKey("reduce_motion")
    private val arabic = booleanPreferencesKey("show_arabic")
    private val mode = stringPreferencesKey("theme_mode")
    private val palette = stringPreferencesKey("palette")
    private val scale = floatPreferencesKey("type_scale")
    private val compact = booleanPreferencesKey("compact_cards")
    private val shapes = booleanPreferencesKey("soft_shapes")
    private val order = stringPreferencesKey("journey_order")
    private val hidden = stringPreferencesKey("journey_hidden")

    val preferences: Flow<NurPreferences> = context.nurDataStore.data.map { p ->
        val selectedMode = p[mode] ?: if (p[dark] ?: true) "dark" else "light"
        val selectedPalette = p[palette] ?: if (p[gold] ?: true) "gold" else "ocean"
        NurPreferences(
            darkMode = selectedMode != "light", dynamicColor = p[dynamic] ?: false,
            goldAccent = selectedPalette == "gold", reduceMotion = p[motion] ?: false,
            showArabic = p[arabic] ?: true, themeMode = selectedMode,
            palette = selectedPalette, typeScale = (p[scale] ?: 1f).coerceIn(0.85f, 1.2f),
            compactCards = p[compact] ?: false, softShapes = p[shapes] ?: true,
            journey = JourneyLayout.restore(p[order], p[hidden])
        )
    }

    suspend fun update(key: String, value: Boolean) {
        context.nurDataStore.edit { p ->
            when (key) {
                "dark" -> { p[dark] = value; p[mode] = if (value) "dark" else "light" }
                "dynamic" -> p[dynamic] = value
                "gold" -> { p[gold] = value; p[palette] = if (value) "gold" else "ocean" }
                "motion" -> p[motion] = value
                "arabic" -> p[arabic] = value
                "compact" -> p[compact] = value
                "shapes" -> p[shapes] = value
            }
        }
    }

    suspend fun updateChoice(key: String, value: String) {
        context.nurDataStore.edit { p ->
            when (key) {
                "mode" -> if (value in listOf("system", "light", "dark")) {
                    p[mode] = value; p[dark] = value != "light"
                }
                "palette" -> if (value in listOf("gold", "ocean", "sage", "rose")) {
                    p[palette] = value; p[gold] = value == "gold"
                }
            }
        }
    }

    suspend fun updateScale(value: Float) {
        context.nurDataStore.edit { it[scale] = value.coerceIn(0.85f, 1.2f) }
    }

    suspend fun saveJourney(layout: JourneyLayout) {
        context.nurDataStore.edit { p ->
            p[order] = layout.order.joinToString(",")
            p[hidden] = layout.hidden.joinToString(",")
        }
    }

    suspend fun resetAppearance() {
        context.nurDataStore.edit { p ->
            listOf(dark, dynamic, gold, motion, arabic, compact, shapes).forEach { p.remove(it) }
            listOf(mode, palette, order, hidden).forEach { p.remove(it) }
            p.remove(scale)
        }
    }
}
