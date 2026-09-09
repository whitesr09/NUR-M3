package com.nshd.nurm3.data

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** Synchronous locale preference is available before Activity resources are attached. */
data class NurAccessibility(
    val language: String = "system",
    val textScale: Float = 1f,
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = false,
    val showHints: Boolean = true
)

class NurAccessibilityStore private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("nur_accessibility", Context.MODE_PRIVATE)
    private fun load() = NurAccessibility(
        language = prefs.getString("language", "system")?.takeIf { it in languages } ?: "system",
        textScale = prefs.getFloat("text_scale", 1f).takeIf { it.isFinite() }?.coerceIn(1f, 1.6f) ?: 1f,
        highContrast = prefs.getBoolean("high_contrast", false),
        reduceMotion = prefs.getBoolean("reduce_motion", false),
        showHints = prefs.getBoolean("show_hints", true)
    )
    private val state = MutableStateFlow(load())
    val settings: StateFlow<NurAccessibility> = state.asStateFlow()
    fun update(value: NurAccessibility) {
        val safe = value.copy(language = value.language.takeIf { it in languages } ?: "system", textScale = value.textScale.takeIf { it.isFinite() }?.coerceIn(1f, 1.6f) ?: 1f)
        check(prefs.edit().putString("language", safe.language).putFloat("text_scale", safe.textScale)
            .putBoolean("high_contrast", safe.highContrast).putBoolean("reduce_motion", safe.reduceMotion)
            .putBoolean("show_hints", safe.showHints).commit()) { "Could not save accessibility settings." }
        state.value = safe
    }
    companion object {
        val languages = setOf("system", "en", "ml", "ar")
        @Volatile private var instance: NurAccessibilityStore? = null
        fun get(context: Context): NurAccessibilityStore = instance ?: synchronized(this) {
            instance ?: NurAccessibilityStore(context).also { instance = it }
        }
        fun localized(base: Context): Context {
            val language = base.getSharedPreferences("nur_accessibility", Context.MODE_PRIVATE).getString("language", "system")
            if (language !in languages || language == "system") return base
            val locale = Locale.forLanguageTag(language!!)
            val config = Configuration(base.resources.configuration)
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            return base.createConfigurationContext(config)
        }
    }
}
