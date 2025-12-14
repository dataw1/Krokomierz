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
        // UI Preferences
        val DARK_THEME_KEY = booleanPreferencesKey("dark_theme_key")
        val IS_METRIC_KEY = booleanPreferencesKey("is_metric_key")
        val STEP_GOAL_KEY = intPreferencesKey("step_goal_key")
        val USER_NAME_KEY = stringPreferencesKey("user_name_key")
        val LAST_USER_ID_KEY = stringPreferencesKey("last_user_id")

        // State for StepCounter internal logic
        val KEY_INITIAL_STEPS_FOR_SESSION = intPreferencesKey("initial_steps_for_session")
        val KEY_LAST_COUNT_DATE = stringPreferencesKey("last_count_date")
        
        // State for UI to observe
        val KEY_STEPS_FOR_UI = intPreferencesKey("steps_for_ui")

        // State for Firebase sync logic
        val KEY_DATE_FOR_SAVED_STEPS_FIREBASE = stringPreferencesKey("date_for_saved_steps_firebase")
        val KEY_STEPS_SAVED_ON_DATE_FIREBASE = intPreferencesKey("steps_saved_on_date_firebase")
    }

    // --- UI Preferences ---
    val isDarkTheme: Flow<Boolean> = dataStore.data.map { it[DARK_THEME_KEY] ?: false }
    suspend fun setDarkTheme(isDark: Boolean) = dataStore.edit { it[DARK_THEME_KEY] = isDark }

    val isMetric: Flow<Boolean> = dataStore.data.map { it[IS_METRIC_KEY] ?: true }
    suspend fun setMetric(isMetric: Boolean) = dataStore.edit { it[IS_METRIC_KEY] = isMetric }

    val stepGoal: Flow<Int> = dataStore.data.map { it[STEP_GOAL_KEY] ?: 10000 }
    suspend fun setStepGoal(goal: Int) = dataStore.edit { it[STEP_GOAL_KEY] = goal }

    val userName: Flow<String?> = dataStore.data.map { it[USER_NAME_KEY] }
    suspend fun setUserName(name: String) = dataStore.edit { it[USER_NAME_KEY] = name }
    
    val lastUserId: Flow<String?> = dataStore.data.map { it[LAST_USER_ID_KEY] }
    suspend fun setLastUserId(userId: String?) {
        dataStore.edit {
            if (userId == null) it.remove(LAST_USER_ID_KEY) else it[LAST_USER_ID_KEY] = userId
        }
    }

    // --- Step Counter State ---
    val steps: Flow<Int> = dataStore.data.map { it[KEY_STEPS_FOR_UI] ?: 0 }
    suspend fun updateUiSteps(newSteps: Int) {
        dataStore.edit { it[KEY_STEPS_FOR_UI] = newSteps }
    }
    
    val initialSteps: Flow<Int> = dataStore.data.map { it[KEY_INITIAL_STEPS_FOR_SESSION] ?: 0 }
    val lastCountDate: Flow<String> = dataStore.data.map { it[KEY_LAST_COUNT_DATE] ?: "" }
    suspend fun saveStepCounterSessionState(initial: Int, date: String) {
        dataStore.edit {
            it[KEY_INITIAL_STEPS_FOR_SESSION] = initial
            it[KEY_LAST_COUNT_DATE] = date
        }
    }

    // --- Firebase Sync State ---
    val dateForSavedSteps: Flow<String> = dataStore.data.map { it[KEY_DATE_FOR_SAVED_STEPS_FIREBASE] ?: "" }
    val stepsSavedOnDate: Flow<Int> = dataStore.data.map { it[KEY_STEPS_SAVED_ON_DATE_FIREBASE] ?: 0 }
    suspend fun updateFirebaseSavedSteps(date: String, steps: Int) {
        dataStore.edit {
            it[KEY_DATE_FOR_SAVED_STEPS_FIREBASE] = date
            it[KEY_STEPS_SAVED_ON_DATE_FIREBASE] = steps
        }
    }

    fun getTodayDateString(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}