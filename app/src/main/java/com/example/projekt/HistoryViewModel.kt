/**
 * @file HistoryViewModel.kt
 * @brief ViewModel zarządzający historią aktywności użytkownika oraz trasami.
 */

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

/**
 * @struct HistoryState
 * @brief Stan interfejsu historii aktywności i tras.
 * 
 * @property hourlyStepsToday Mapa kroków na godzinę dla bieżącego dnia.
 * @property dailyStepsLastWeek Mapa kroków dla każdego dnia ostatniego tygodnia.
 * @property weeklyStepsLastMonth Mapa kroków dla każdego tygodnia ostatniego miesiąca.
 * @property routes Lista zapisanych tras użytkownika.
 * @property selectedRouteForFollowing Opcjonalna trasa wybrana do podążania na mapie.
 * @property isLoading Flaga informująca o trwającym procesie ładowania danych.
 * @property error Opcjonalny komunikat o błędzie.
 */
data class HistoryState(
    val hourlyStepsToday: Map<String, Int> = emptyMap(),
    val dailyStepsLastWeek: Map<String, Int> = emptyMap(),
    val weeklyStepsLastMonth: Map<String, Int> = emptyMap(),
    val routes: List<RouteData> = emptyList(),
    val selectedRouteForFollowing: RouteData? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * @class HistoryViewModel
 * @brief ViewModel obsługujący pobieranie, zapisywanie i usuwanie danych historycznych z Firebase.
 * 
 * Klasa odpowiada za synchronizację danych o krokach oraz trasach GPS z Firebase Realtime Database.
 */
class HistoryViewModel : ViewModel() {

    private val _historyState = MutableStateFlow(HistoryState())
    
    /**
     * @brief StateFlow emitujący aktualny stan historii.
     */
    val historyState: StateFlow<HistoryState> = _historyState

    private val database = Firebase.database
    private val auth = Firebase.auth

    init {
        fetchHistory()
        fetchRoutes()
    }

    /**
     * @brief Pobiera historię kroków użytkownika z Firebase.
     * 
     * Przetwarza dane w celu przygotowania statystyk godzinowych, dziennych i tygodniowych.
     */
    private fun fetchHistory() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val historyRef = database.getReference("userActivity").child(userId)

            historyRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val now = Calendar.getInstance()

                    val todayStartCal = (now.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
                    val sevenDaysAgoCal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7) }
                    val thirtyDaysAgoCal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -30) }

                    val hourlySteps = mutableMapOf<String, Int>()
                    val dailySteps = mutableMapOf<Int, Int>()
                    val weeklySteps = mutableMapOf<Int, Int>()

                    for (child in snapshot.children) {
                        val timestamp = child.child("timestamp").getValue(Long::class.java) ?: continue
                        val steps = child.child("steps").getValue(Int::class.java) ?: continue
                        val recordCal = Calendar.getInstance().apply { timeInMillis = timestamp }

                        // Hourly for Today
                        if (timestamp >= todayStartCal.timeInMillis) {
                            val hour = recordCal.get(Calendar.HOUR_OF_DAY)
                            val hourKey = "${hour.toString().padStart(2, '0')}:00"
                            hourlySteps[hourKey] = (hourlySteps[hourKey] ?: 0) + steps
                        }

                        // Daily for Last Week
                        if (timestamp >= sevenDaysAgoCal.timeInMillis) {
                            val dayOfWeek = recordCal.get(Calendar.DAY_OF_WEEK)
                            dailySteps[dayOfWeek] = (dailySteps[dayOfWeek] ?: 0) + steps
                        }

                        // Weekly for Last Month
                        if (timestamp >= thirtyDaysAgoCal.timeInMillis) {
                            val weekOfMonth = recordCal.get(Calendar.WEEK_OF_MONTH)
                            weeklySteps[weekOfMonth] = (weeklySteps[weekOfMonth] ?: 0) + steps
                        }
                    }

                    _historyState.value = _historyState.value.copy(
                        hourlyStepsToday = hourlySteps.toSortedMap(),
                        dailyStepsLastWeek = mapDays(dailySteps),
                        weeklyStepsLastMonth = weeklySteps.mapKeys { "Tydzień ${it.key}" }.toSortedMap(),
                        isLoading = false
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    _historyState.value = _historyState.value.copy(isLoading = false, error = error.message)
                }
            })
        }
    }

    /**
     * @brief Pobiera listę zapisanych tras użytkownika z Firebase.
     */
    private fun fetchRoutes() {
        val userId = auth.currentUser?.uid ?: return
        val routesRef = database.getReference("userRoutes").child(userId)

        routesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val routesList = mutableListOf<RouteData>()
                for (child in snapshot.children) {
                    val route = child.getValue(RouteData::class.java)
                    if (route != null) {
                        routesList.add(route.copy(id = child.key ?: ""))
                    }
                }
                _historyState.value = _historyState.value.copy(routes = routesList.sortedByDescending { it.timestamp })
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    /**
     * @brief Zapisuje nową trasę w bazie danych Firebase.
     * @param name Nazwa trasy.
     * @param points Lista punktów geograficznych.
     * @param distance Dystans trasy w kilometrach.
     */
    fun saveRoute(name: String, points: List<MyLatLng>, distance: Double) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val routesRef = database.getReference("userRoutes").child(userId)
            val newRouteRef = routesRef.push()
            val routeData = RouteData(
                id = newRouteRef.key ?: "",
                name = name,
                points = points,
                timestamp = System.currentTimeMillis(),
                distanceKm = distance
            )
            newRouteRef.setValue(routeData)
        }
    }

    /**
     * @brief Usuwa zapisaną trasę z bazy danych.
     * @param routeId Identyfikator trasy do usunięcia.
     */
    fun deleteRoute(routeId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            database.getReference("userRoutes").child(userId).child(routeId).removeValue()
        }
    }

    /**
     * @brief Wybiera trasę, która ma być wyświetlona na mapie w celu podążania.
     * @param route Obiekt trasy lub null, aby wyczyścić wybór.
     */
    fun selectRouteForFollowing(route: RouteData?) {
        _historyState.value = _historyState.value.copy(selectedRouteForFollowing = route)
    }

    /**
     * @brief Mapuje numery dni tygodnia na polskie skróty.
     * @param dailySteps Mapa z numerami dni (Calendar).
     * @return Uporządkowana mapa ze skrótami dni.
     */
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
