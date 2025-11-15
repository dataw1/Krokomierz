package com.example.projekt

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Utworzenie DataStore na poziomie aplikacji (Singleton)
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemePreferenceManager(context: Context) {
    private val dataStore = context.dataStore

    // Klucz do zapisu stanu motywu
    companion object {
        val DARK_THEME_KEY = booleanPreferencesKey("dark_theme_key")
    }

    // Odczytuje stan motywu (Flow)
    val isDarkTheme: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[DARK_THEME_KEY] ?: false // Domyślnie na FALSE
        }

    // Zapisuje stan motywu
    suspend fun setDarkTheme(isDark: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_THEME_KEY] = isDark
        }
    }
}