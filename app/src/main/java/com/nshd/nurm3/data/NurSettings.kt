package com.nshd.nurm3.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.nshd.nurm3.ui.JourneyLayout
import com.nshd.nurm3.ui.JourneyOptions
import com.nshd.nurm3.ui.JourneyPreset
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
    val journey: JourneyLayout = JourneyLayout.DEFAULT,
    val appLock: Boolean = false,
    val privatePreview: Boolean = true,
    val backupReminders: Boolean = true,
    val widgetsEnabled: Boolean = true,
    val companionModules: Boolean = false,
    val nurAiEnabled: Boolean = false,
    val dhikrHaptics: Boolean = true,
    val progressStyle: String = "slim",
    val highRefreshRate: Boolean = false,
    val journeyOptions: JourneyOptions = JourneyOptions(),
    val savedJourneyPreset: JourneyPreset? = null,
    val fontStyle: String = "default",
    val customFontId: String = "",
    val customFontName: String = ""
)

object NurFontChoices {
    val styles = listOf("default", "sans", "serif", "mono", "rounded", "custom")
    fun normalize(value: String?): String = value?.takeIf { it in styles } ?: "default"
}

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
    private val appLock = booleanPreferencesKey("app_lock")
    private val privatePreview = booleanPreferencesKey("private_preview")
    private val backupReminders = booleanPreferencesKey("backup_reminders")
    private val widgetsEnabled = booleanPreferencesKey("widgets_enabled")
    private val companionModules = booleanPreferencesKey("companion_modules")
    private val nurAiEnabled = booleanPreferencesKey("nur_ai_enabled")
    private val dhikrHaptics = booleanPreferencesKey("dhikr_haptics")
    private val progressStyle = stringPreferencesKey("progress_style")
    private val highRefreshRate = booleanPreferencesKey("high_refresh_rate")
    private val cardSizes = stringPreferencesKey("journey_card_sizes")
    private val cardPinned = stringPreferencesKey("journey_card_pinned")
    private val cardCollapsed = stringPreferencesKey("journey_card_collapsed")
    private val presetName = stringPreferencesKey("journey_preset_name")
    private val presetOrder = stringPreferencesKey("journey_preset_order")
    private val presetHidden = stringPreferencesKey("journey_preset_hidden")
    private val presetSizes = stringPreferencesKey("journey_preset_sizes")
    private val presetPinned = stringPreferencesKey("journey_preset_pinned")
    private val presetCollapsed = stringPreferencesKey("journey_preset_collapsed")
    private val fontStyle = stringPreferencesKey("font_style")
    private val customFontId = stringPreferencesKey("custom_font_id")
    private val customFontName = stringPreferencesKey("custom_font_name")

    private fun options(p: Preferences) = JourneyOptions.restore(p[cardSizes], p[cardPinned], p[cardCollapsed])
    private fun writeOptions(p: MutablePreferences, value: JourneyOptions) {
        p[cardSizes] = value.encodeSizes()
        p[cardPinned] = value.encodePinned()
        p[cardCollapsed] = value.encodeCollapsed()
    }
    private fun writeJourney(p: MutablePreferences, layout: JourneyLayout) {
        p[order] = layout.order.joinToString(",")
        p[hidden] = layout.hidden.joinToString(",")
    }
    private fun savedPreset(p: Preferences): JourneyPreset? {
        val name = p[presetName]?.takeIf { JourneyPreset.validName(it) } ?: return null
        return JourneyPreset(name, JourneyLayout.restore(p[presetOrder], p[presetHidden]),
            JourneyOptions.restore(p[presetSizes], p[presetPinned], p[presetCollapsed]))
    }

    val preferences: Flow<NurPreferences> = context.nurDataStore.data.map { p ->
        val selectedMode = AppearanceChoices.mode(p[mode] ?: if (p[dark] ?: true) "dark" else "light")
        val selectedPalette = p[palette]?.takeIf { it in setOf("gold", "ocean", "sage", "rose") } ?: if (p[gold] ?: true) "gold" else "ocean"
        NurPreferences(
            darkMode = selectedMode == "dark" || selectedMode == "amoled" || selectedMode == "system" && (p[dark] ?: true),
            dynamicColor = p[dynamic] ?: false,
            goldAccent = selectedPalette == "gold",
            reduceMotion = p[motion] ?: false,
            showArabic = p[arabic] ?: true,
            themeMode = selectedMode,
            palette = selectedPalette,
            typeScale = (p[scale] ?: 1f).takeIf { it.isFinite() }?.coerceIn(0.85f, 1.2f) ?: 1f,
            compactCards = p[compact] ?: false,
            softShapes = p[shapes] ?: true,
            journey = JourneyLayout.restore(p[order], p[hidden]),
            appLock = p[appLock] ?: false,
            privatePreview = p[privatePreview] ?: true,
            backupReminders = p[backupReminders] ?: true,
            widgetsEnabled = p[widgetsEnabled] ?: true,
            companionModules = p[companionModules] ?: false,
            nurAiEnabled = p[nurAiEnabled] ?: false,
            dhikrHaptics = p[dhikrHaptics] ?: true,
            progressStyle = AppearanceChoices.progress(p[progressStyle]),
            highRefreshRate = p[highRefreshRate] ?: false,
            journeyOptions = options(p),
            savedJourneyPreset = savedPreset(p),
            fontStyle = NurFontChoices.normalize(p[fontStyle]),
            customFontId = p[customFontId].orEmpty().takeIf { it.matches(Regex("[a-f0-9]{64}")) }.orEmpty(),
            customFontName = p[customFontName].orEmpty().take(100)
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
                "app_lock" -> p[appLock] = value
                "private_preview" -> p[privatePreview] = value
                "backup_reminders" -> p[backupReminders] = value
                "widgets" -> p[widgetsEnabled] = value
                "companion_modules" -> p[companionModules] = value
                "nur_ai" -> p[nurAiEnabled] = value
                "dhikr_haptics" -> p[dhikrHaptics] = value
                "high_refresh_rate" -> p[highRefreshRate] = value
            }
        }
    }

    suspend fun updateChoice(key: String, value: String) {
        context.nurDataStore.edit { p ->
            when (key) {
                "mode" -> if (value in AppearanceChoices.modes) { p[mode] = value; p[dark] = value != "light" }
                "palette" -> if (value in listOf("gold", "ocean", "sage", "rose")) { p[palette] = value; p[gold] = value == "gold" }
                "progress_style" -> if (value in AppearanceChoices.progressStyles) p[progressStyle] = value
                "font_style" -> if (value in NurFontChoices.styles && (value != "custom" || !p[customFontId].isNullOrBlank())) p[fontStyle] = value
            }
        }
    }
    suspend fun selectCustomFont(id: String, name: String) {
        require(id.matches(Regex("[a-f0-9]{64}")))
        context.nurDataStore.edit { p ->
            p[customFontId] = id
            p[customFontName] = name.take(100)
            p[fontStyle] = "custom"
        }
    }
    suspend fun updateScale(value: Float) {
        context.nurDataStore.edit { it[scale] = if (value.isFinite()) value.coerceIn(0.85f, 1.2f) else 1f }
    }
    suspend fun saveJourney(layout: JourneyLayout) {
        context.nurDataStore.edit { writeJourney(it, layout) }
    }
    suspend fun updateJourneyOptions(transform: (JourneyOptions) -> JourneyOptions) {
        context.nurDataStore.edit { p -> writeOptions(p, transform(options(p))) }
    }
    suspend fun saveJourneyPreset(name: String) {
        require(JourneyPreset.validName(name))
        context.nurDataStore.edit { p ->
            val current = JourneyLayout.restore(p[order], p[hidden])
            val selected = options(p)
            p[presetName] = name.trim()
            p[presetOrder] = current.order.joinToString(",")
            p[presetHidden] = current.hidden.joinToString(",")
            p[presetSizes] = selected.encodeSizes()
            p[presetPinned] = selected.encodePinned()
            p[presetCollapsed] = selected.encodeCollapsed()
        }
    }
    suspend fun applyJourneyPreset() {
        context.nurDataStore.edit { p ->
            val preset = savedPreset(p) ?: return@edit
            writeJourney(p, preset.layout)
            writeOptions(p, preset.options)
        }
    }
    suspend fun applyJourneyLayout(layout: JourneyLayout, selected: JourneyOptions = JourneyOptions()) {
        context.nurDataStore.edit { p -> writeJourney(p, layout); writeOptions(p, selected) }
    }
    suspend fun resetAppearance() {
        context.nurDataStore.edit { p ->
            listOf(dark, dynamic, gold, motion, arabic, compact, shapes, highRefreshRate).forEach { p.remove(it) }
            listOf(mode, palette, order, hidden, progressStyle, cardSizes, cardPinned, cardCollapsed, fontStyle).forEach { p.remove(it) }
            p.remove(scale)
        }
    }
}
