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
                    // 1. Get all records and sort them by timestamp
                    val allRecords = snapshot.children.mapNotNull {
                        val ts = it.child("timestamp").getValue(Long::class.java)
                        val steps = it.child("steps").getValue(Int::class.java)
                        if (ts != null && steps != null) ts to steps else null
                    }.sortedBy { it.first }

                    if (allRecords.isEmpty()) {
                        _historyState.value = HistoryState(isLoading = false)
                        return
                    }

                    // 2. Group records by different time windows
                    val now = Calendar.getInstance()
                    val todayStartCal = (now.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                    val sevenDaysAgoCal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7) }
                    val thirtyDaysAgoCal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -30) }

                    // HOURLY
                    val todayRecords = allRecords.filter { it.first >= todayStartCal.timeInMillis }
                    val hourlySteps = todayRecords.groupBy {
                        val cal = Calendar.getInstance().apply { timeInMillis = it.first }
                        cal.get(Calendar.HOUR_OF_DAY)
                    }.mapValues { (_, records) ->
                        calculateTrueSteps(records)
                    }.mapKeys { "${it.key.toString().padStart(2, '0')}:00" }.toSortedMap()

                    // DAILY
                    val lastWeekRecords = allRecords.filter { it.first >= sevenDaysAgoCal.timeInMillis }
                    val dailySteps = lastWeekRecords.groupBy {
                        val cal = Calendar.getInstance().apply { timeInMillis = it.first }
                        cal.get(Calendar.DAY_OF_WEEK)
                    }.mapValues { (_, records) ->
                        calculateTrueSteps(records)
                    }

                    // WEEKLY
                    val lastMonthRecords = allRecords.filter { it.first >= thirtyDaysAgoCal.timeInMillis }
                    val weeklySteps = lastMonthRecords.groupBy {
                        val cal = Calendar.getInstance().apply { timeInMillis = it.first }
                        cal.get(Calendar.WEEK_OF_MONTH)
                    }.mapValues { (_, records) ->
                        calculateTrueSteps(records)
                    }

                    // 3. Update state
                    _historyState.value = HistoryState(
                        hourlyStepsToday = hourlySteps,
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
    
    private fun calculateTrueSteps(records: List<Pair<Long, Int>>): Int {
        if (records.isEmpty()) return 0
        var total = 0
        var lastSteps = 0
        for ((_, steps) in records) {
            if (steps < lastSteps) { // New session detected
                total += lastSteps
            }
            lastSteps = steps
        }
        total += lastSteps // Add the last/current session's steps
        return total
    }

    private fun mapDays(dailySteps: Map<Int, Int>): Map<String, Int> {
        val orderedResult = linkedMapOf<String, Int>()
        orderedResult["Pon"] = dailySteps[Calendar.MONDAY] ?: 0
        orderedResult["Wto"] = dailySteps[Calendar.TUESDAY] ?: 0
        orderedResult["Śro"] = dailySteps[Calendar.WEDNESDAY] ?: 0
        orderedResult["Czw"] = dailySteps[Calendar.THURSDAY] ?: 0
        orderedResult["Pią"] = dailySteps[Calendar.FRIDAY] ?: 0
        orderedResult["Sob"] = dailySteps[Calendar.SATURDAY] ?: 0
        orderedResult["Nie"] = dailySteps[Calendar.SUNDAY] ?: 0
        return orderedResult
    }
}