package com.example.projekt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

data class HistoryState(
    val hourlyStepsToday: Map<String, Int> = emptyMap(),
    val dailyStepsLastWeek: Map<String, Int> = emptyMap(),
    val weeklyStepsLastMonth: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class HistoryViewModel : ViewModel() {

    private val _historyState = MutableStateFlow(HistoryState())
    val historyState: StateFlow<HistoryState> = _historyState

    private val database = Firebase.database
    private val auth = Firebase.auth

    init {
        fetchHistory()
    }

    private fun fetchHistory() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val historyRef = database.getReference("userActivity").child(userId)

            historyRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val now = Calendar.getInstance()
                    val startOfToday = (now.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val sevenDaysAgo = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
                    val thirtyDaysAgo = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis

                    val hourlySteps = mutableMapOf<String, Int>()
                    val dailySteps = mutableMapOf<Int, Int>()
                    val weeklySteps = mutableMapOf<Int, Int>()

                    for (child in snapshot.children) {
                        val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0
                        val steps = child.child("steps").getValue(Int::class.java) ?: 0
                        val recordCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }

                        if (timestamp >= startOfToday) {
                            val hour = recordCalendar.get(Calendar.HOUR_OF_DAY)
                            val hourKey = "${hour.toString().padStart(2, '0')}"
                            hourlySteps[hourKey] = (hourlySteps[hourKey] ?: 0) + steps
                        }

                        if (timestamp >= sevenDaysAgo) {
                            val dayOfWeek = recordCalendar.get(Calendar.DAY_OF_WEEK) // Sunday = 1, Saturday = 7
                            dailySteps[dayOfWeek] = (dailySteps[dayOfWeek] ?: 0) + steps
                        }

                        if (timestamp >= thirtyDaysAgo) {
                            val weekOfMonth = recordCalendar.get(Calendar.WEEK_OF_MONTH)
                            weeklySteps[weekOfMonth] = (weeklySteps[weekOfMonth] ?: 0) + steps
                        }
                    }

                    _historyState.value = HistoryState(
                        hourlyStepsToday = hourlySteps.toSortedMap(),
                        dailyStepsLastWeek = mapDays(dailySteps),
                        weeklyStepsLastMonth = weeklySteps.mapKeys { "Tydzień ${it.key}" }.toSortedMap(),
                        isLoading = false
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    _historyState.value = HistoryState(isLoading = false, error = error.message)
                }
            })
        }
    }

    private fun mapDays(dailySteps: Map<Int, Int>): Map<String, Int> {
        val dayMapping = listOf(
            Calendar.MONDAY to "Pon",
            Calendar.TUESDAY to "Wto",
            Calendar.WEDNESDAY to "Śro",
            Calendar.THURSDAY to "Czw",
            Calendar.FRIDAY to "Pią",
            Calendar.SATURDAY to "Sob",
            Calendar.SUNDAY to "Nie"
        )
        
        val remapped = dailySteps.mapKeys { 
            when(it.key) {
                Calendar.MONDAY -> "Pon"
                Calendar.TUESDAY -> "Wto"
                Calendar.WEDNESDAY -> "Śro"
                Calendar.THURSDAY -> "Czw"
                Calendar.FRIDAY -> "Pią"
                Calendar.SATURDAY -> "Sob"
                Calendar.SUNDAY -> "Nie"
                else -> ""
            }
        }

        return dayMapping.map { it.second to (remapped[it.second] ?: 0) }.toMap()
    }
}