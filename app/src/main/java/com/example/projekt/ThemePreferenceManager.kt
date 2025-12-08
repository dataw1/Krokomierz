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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemePreferenceManager(context: Context) {
    private val dataStore = context.dataStore

    companion object {
        val DARK_THEME_KEY = booleanPreferencesKey("dark_theme_key")
        val IS_METRIC_KEY = booleanPreferencesKey("is_metric_key")
        val STEP_GOAL_KEY = intPreferencesKey("step_goal_key")
        val USER_NAME_KEY = stringPreferencesKey("user_name_key")

        // Keys for persisting step counter state
        val KEY_INITIAL_STEPS = intPreferencesKey("initial_steps")
        val KEY_LAST_COUNT_DATE = stringPreferencesKey("last_count_date")
        val KEY_PREVIOUS_DAY_STEPS = intPreferencesKey("previous_day_steps")
    }

    val isDarkTheme: Flow<Boolean> = dataStore.data.map { it[DARK_THEME_KEY] ?: false }
    suspend fun setDarkTheme(isDark: Boolean) = dataStore.edit { it[DARK_THEME_KEY] = isDark }

    val isMetric: Flow<Boolean> = dataStore.data.map { it[IS_METRIC_KEY] ?: true }
    suspend fun setMetric(isMetric: Boolean) = dataStore.edit { it[IS_METRIC_KEY] = isMetric }

    val stepGoal: Flow<Int> = dataStore.data.map { it[STEP_GOAL_KEY] ?: 10000 }
    suspend fun setStepGoal(goal: Int) = dataStore.edit { it[STEP_GOAL_KEY] = goal }

    val userName: Flow<String?> = dataStore.data.map { it[USER_NAME_KEY] }
    suspend fun setUserName(name: String) = dataStore.edit { it[USER_NAME_KEY] = name }

    // Flow for step counter data
    val initialSteps: Flow<Int> = dataStore.data.map { it[KEY_INITIAL_STEPS] ?: 0 }
    val lastCountDate: Flow<String> = dataStore.data.map { it[KEY_LAST_COUNT_DATE] ?: "" }
    val previousDaySteps: Flow<Int> = dataStore.data.map { it[KEY_PREVIOUS_DAY_STEPS] ?: 0 }

    suspend fun saveStepCounterState(initial: Int, date: String, previousSteps: Int) {
        dataStore.edit {
            it[KEY_INITIAL_STEPS] = initial
            it[KEY_LAST_COUNT_DATE] = date
            it[KEY_PREVIOUS_DAY_STEPS] = previousSteps
        }
    }

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}