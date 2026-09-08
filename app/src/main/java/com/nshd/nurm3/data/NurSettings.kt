package com.nshd.nurm3.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.nurDataStore by preferencesDataStore(name = "nur_settings")

data class NurPreferences(
    val darkMode: Boolean = true,
    val dynamicColor: Boolean = false,
    val goldAccent: Boolean = true,
    val reduceMotion: Boolean = false,
    val showArabic: Boolean = true
)

class NurSettings(private val context: Context) {
    private val dark = booleanPreferencesKey("dark_mode")
    private val dynamic = booleanPreferencesKey("dynamic_color")
    private val gold = booleanPreferencesKey("gold_accent")
    private val motion = booleanPreferencesKey("reduce_motion")
    private val arabic = booleanPreferencesKey("show_arabic")

    val preferences: Flow<NurPreferences> = context.nurDataStore.data.map { p ->
        NurPreferences(p[dark] ?: true, p[dynamic] ?: false, p[gold] ?: true,
            p[motion] ?: false, p[arabic] ?: true)
    }

    suspend fun update(key: String, value: Boolean) {
        context.nurDataStore.edit { p ->
            when (key) {
                "dark" -> p[dark] = value
                "dynamic" -> p[dynamic] = value
                "gold" -> p[gold] = value
                "motion" -> p[motion] = value
                "arabic" -> p[arabic] = value
            }
        }
    }
}
