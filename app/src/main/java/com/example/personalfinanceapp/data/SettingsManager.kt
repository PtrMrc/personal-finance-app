package com.example.personalfinanceapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property for DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * The three available app themes.
 * Stored as an Int in DataStore for simplicity and backward compatibility.
 *
 * LIGHT  (0) — light mode
 * SIMPLE (1) — dark teal-slate inspired by Simple Pay (default, maps from old darkMode=true)
 * OLED   (2) — true black, max battery saving on OLED screens
 */
enum class AppTheme(val value: Int) {
    LIGHT(0),
    SIMPLE(1),
    OLED(2);

    companion object {
        fun fromValue(value: Int): AppTheme = entries.firstOrNull { it.value == value } ?: SIMPLE
    }
}

/**
 * Settings manager for app preferences.
 * Uses DataStore with an int key so all three themes are stored in a single value.
 *
 * Backward compatibility: old boolean dark mode (true) maps to NAVY (1).
 */
class SettingsManager(private val context: Context) {

    companion object {
        // Int key replaces the old boolean key — stores AppTheme.value
        // Named differently from the old "dark_mode" boolean key to avoid type conflicts
        private val THEME_KEY = intPreferencesKey("app_theme")
    }

    /**
     * Get the current theme as a Flow.
     * Defaults to NAVY (1) so existing users who had dark mode enabled
     * seamlessly land on the navy dark theme.
     */
    val themeFlow: Flow<AppTheme> = context.dataStore.data
        .map { preferences ->
            val raw = preferences[THEME_KEY] ?: AppTheme.SIMPLE.value
            AppTheme.fromValue(raw)
        }

    /**
     * Set the app theme explicitly.
     */
    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.value
        }
    }
}