package com.example.projekt

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemePreferenceManager(context: Context) {
    private val dataStore = context.dataStore

    companion object {
        val DARK_THEME_KEY = booleanPreferencesKey("dark_theme_key")
        val IS_METRIC_KEY = booleanPreferencesKey("is_metric_key")
        val STEP_GOAL_KEY = intPreferencesKey("step_goal_key")
        val USER_NAME_KEY = stringPreferencesKey("user_name_key")
    }

    val isDarkTheme: Flow<Boolean> = dataStore.data.map {
        it[DARK_THEME_KEY] ?: false
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        dataStore.edit { it[DARK_THEME_KEY] = isDark }
    }

    val isMetric: Flow<Boolean> = dataStore.data.map {
        it[IS_METRIC_KEY] ?: true
    }

    suspend fun setMetric(isMetric: Boolean) {
        dataStore.edit { it[IS_METRIC_KEY] = isMetric }
    }

    val stepGoal: Flow<Int> = dataStore.data.map {
        it[STEP_GOAL_KEY] ?: 10000 // Default step goal
    }

    suspend fun setStepGoal(goal: Int) {
        dataStore.edit { it[STEP_GOAL_KEY] = goal }
    }

    val userName: Flow<String?> = dataStore.data.map {
        it[USER_NAME_KEY]
    }

    suspend fun setUserName(name: String) {
        dataStore.edit { it[USER_NAME_KEY] = name }
    }
}