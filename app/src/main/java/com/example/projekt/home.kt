/**
 * @file home.kt
 * @brief Ekran główny aplikacji wyświetlający podsumowanie aktywności.
 */

package com.example.projekt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.projekt.ui.theme.ProjektTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * @enum TimePeriod
 * @brief Okresy czasu do wyświetlania statystyk kroków.
 * 
 * @property label Etykieta wyświetlana w interfejsie użytkownika.
 */
enum class TimePeriod(val label: String) {
    DAY("Dzień"),
    WEEK("Tydzień"),
    MONTH("Miesiąc")
}

/**
 * @brief Główny komponent ekranu HomeScreen.
 * 
 * Wyświetla powitanie, aktualną datę, przełącznik okresów czasu (Dzień, Tydzień, Miesiąc)
 * oraz szczegółowe statystyki kroków pobrane z [HistoryViewModel].
 *
 * @param userName Imię/nazwa użytkownika.
 * @param useMetric Czy używać systemu metrycznego (km) czy imperialnego (mi).
 * @param steps Aktualna liczba kroków z czujnika.
 * @param stepGoal Dzienny cel kroków.
 * @param modifier Modyfikator układu.
 * @param historyViewModel ViewModel dostarczający dane historyczne.
 */
@Composable
fun HomeScreen(
    userName: String,
    useMetric: Boolean,
    steps: Int,
    stepGoal: Int,
    modifier: Modifier = Modifier,
    historyViewModel: HistoryViewModel = viewModel()
) {
    var selectedPeriod by remember { mutableStateOf(TimePeriod.DAY) }
    val historyState by historyViewModel.historyState.collectAsState()

    val dayOfWeek = getCurrentDayOfWeek()
    val weekOfMonth = getWeekOfMonth()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Witaj, $userName!",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "$dayOfWeek, $weekOfMonth tydzień miesiąca",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            TimePeriod.entries.forEach { period ->
                val isSelected = selectedPeriod == period
                AssistChip(
                    onClick = { selectedPeriod = period },
                    label = { Text(period.label) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (historyState.isLoading) {
            CircularProgressIndicator()
        } else if (historyState.error != null) {
            Text(
                text = "Błąd: ${historyState.error}",
                color = MaterialTheme.colorScheme.error
            )
        } else {
            when (selectedPeriod) {
                TimePeriod.DAY -> {
                    Column {
                        DailySummaryCard(steps = steps, stepGoal = stepGoal, useMetric = useMetric)
                        Spacer(Modifier.height(16.dp))
                        StepDetailsList(data = historyState.hourlyStepsToday, title = "Dzisiejsze kroki (godzinowo)")
                    }
                }
                TimePeriod.WEEK -> {
                    StepDetailsList(data = historyState.dailyStepsLastWeek, title = "Kroki w ostatnim tygodniu")
                }
                TimePeriod.MONTH -> {
                    StepDetailsList(data = historyState.weeklyStepsLastMonth, title = "Kroki w ostatnim miesiącu")
                }
            }
        }
    }
}

/**
 * @brief Karta podsumowania dziennej aktywności.
 * 
 * Wyświetla liczbę kroków, postęp względem celu, dystans i spalone kalorie.
 *
 * @param steps Liczba kroków.
 * @param stepGoal Cel kroków.
 * @param useMetric Jednostki miary.
 */
@Composable
fun DailySummaryCard(steps: Int, stepGoal: Int, useMetric: Boolean) {
    val stepLengthCm = 80 // Długość kroku w cm
    val distance = if (useMetric) (steps * stepLengthCm) / 100_000.0 else (steps * stepLengthCm) / 160_934.0
    val distanceFormatted = "%.2f".format(distance)
    val distanceUnit = if (useMetric) "km" else "mi"
    val caloriesBurned = steps * 0.04f

    Card(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Kroki Dzisiaj", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                text = steps.toString(),
                fontSize = 52.sp,
                color = MaterialTheme.colorScheme.primary
            )
            if (steps >= stepGoal) {
                Text(
                    "Cel dzienny osiągnięty!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            } else {
                Text("Cel: $stepGoal kroków", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            Text("Dystans: $distanceFormatted $distanceUnit", style = MaterialTheme.typography.bodyMedium)
            Text("Spalone kalorie: %.0f kcal".format(caloriesBurned), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { (steps.toFloat() / stepGoal.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(12.dp),
                color = if (steps >= stepGoal) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * @brief Lista szczegółowa kroków (np. godzinowa lub dzienna).
 * 
 * @param data Mapa z etykietą i odpowiadającą jej liczbą kroków.
 * @param title Tytuł sekcji.
 */
@Composable
fun StepDetailsList(data: Map<String, Int>, title: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            if (data.isEmpty()) {
                Text("Brak danych do wyświetlenia.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    items(data.entries.toList()) { (label, steps) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                            Text("$steps kroków", style = MaterialTheme.typography.bodyMedium)
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

/**
 * @brief Zwraca nazwę bieżącego dnia tygodnia w języku polskim.
 */
private fun getCurrentDayOfWeek(): String {
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("EEEE", Locale("pl", "PL"))
    return dateFormat.format(calendar.time).replaceFirstChar { it.titlecase(Locale.ROOT) }
}

/**
 * @brief Zwraca numer bieżącego tygodnia w miesiącu.
 */
private fun getWeekOfMonth(): Int {
    val calendar = Calendar.getInstance()
    return calendar.get(Calendar.WEEK_OF_MONTH)
}

/**
 * @brief Podgląd ekranu HomeScreen dla środowiska Android Studio.
 */
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    ProjektTheme {
        HomeScreen(userName = "Użytkownik", useMetric = true, steps = 12345, stepGoal = 10000)
    }
}
