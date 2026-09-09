package com.nshd.nurm3.data

import android.content.Context
import com.nshd.nurm3.ui.NurGlassStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lightweight appearance-only glass settings that update immediately without touching user records. */
data class NurGlassPreferences(
    val mode: String = "off",
    val intensity: Float = 0.58f,
    val radius: Int = 22
) {
    val style: NurGlassStyle get() = NurGlassStyle.normalize(mode, intensity, radius)
}

class NurGlassSettingsStore private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("nur_glass_settings", Context.MODE_PRIVATE)

    private fun load() = NurGlassPreferences(
        mode = prefs.getString("mode", "off") ?: "off",
        intensity = prefs.getFloat("intensity", 0.58f),
        radius = prefs.getInt("radius", 22)
    ).let { NurGlassPreferences(it.style.mode, it.style.intensity, it.style.radius) }

    private val state = MutableStateFlow(load())
    val settings: StateFlow<NurGlassPreferences> = state.asStateFlow()

    fun setMode(value: String) = update(state.value.copy(mode = value))
    fun setIntensity(value: Float) = update(state.value.copy(intensity = value))
    fun setRadius(value: Int) = update(state.value.copy(radius = value))
    fun reset() = update(NurGlassPreferences())

    private fun update(value: NurGlassPreferences) {
        val style = value.style
        val safe = NurGlassPreferences(style.mode, style.intensity, style.radius)
        check(
            prefs.edit()
                .putString("mode", safe.mode)
                .putFloat("intensity", safe.intensity)
                .putInt("radius", safe.radius)
                .commit()
        ) { "Could not save glass appearance settings." }
        state.value = safe
    }

    companion object {
        @Volatile private var instance: NurGlassSettingsStore? = null
        fun get(context: Context): NurGlassSettingsStore = instance ?: synchronized(this) {
            instance ?: NurGlassSettingsStore(context).also { instance = it }
        }
    }
}
