package com.example.projekt

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.projekt.ui.theme.ProjektTheme
import androidx.compose.runtime.remember
import androidx.compose.material3.ChipColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

// Enumerator do zarządzania widokiem wykresu z polskimi etykietami
enum class TimePeriod(val label: String) {
    DAY("Dzień"),
    WEEK("Tydzień"),
    MONTH("Miesiąc")
}

// Model danych dla wykresu (uproszczony)
data class StepData(val label: String, var steps: Int)

@Composable
fun HomeScreen(
    userName: String,
    useMetric: Boolean,
    steps: Int,
    stepGoal: Int,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf(TimePeriod.DAY) }
    val stepLengthCm = 80 // Długość kroku w cm

    var currentTime by remember { mutableStateOf("") }
    val chartData = remember { mutableStateListOf<StepData>() }

    // This effect now correctly updates the stateful chart data
    LaunchedEffect(steps, selectedPeriod) {
        updateChartData(chartData, steps, selectedPeriod)
    }

    // LaunchedEffect to update the time every second
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        while (true) {
            currentTime = sdf.format(Date())
            delay(1000)
        }
    }

    val distance = if (useMetric) {
        (steps * stepLengthCm) / 100_000.0 // km
    } else {
        (steps * stepLengthCm) / 160_934.0 // mile
    }
    val distanceFormatted = "%.2f".format(distance)
    val distanceUnit = if (useMetric) "km" else "mi"
    val caloriesBurned = steps * 0.04f

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = currentTime,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Witaj, $userName!",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
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

        // The chips for period selection are now always visible
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            TimePeriod.entries.forEach { period ->
                val isSelected = selectedPeriod == period
                val chipContainerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                val chipLabelColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                AssistChip(
                    onClick = { selectedPeriod = period },
                    label = { Text(period.label) },
                    colors = ChipColors(
                        containerColor = chipContainerColor,
                        labelColor = chipLabelColor,
                        leadingIconContentColor = chipLabelColor,
                        trailingIconContentColor = chipLabelColor,
                        disabledContainerColor = Color.LightGray.copy(alpha = 0.5f),
                        disabledLabelColor = Color.DarkGray.copy(alpha = 0.5f),
                        disabledLeadingIconContentColor = Color.DarkGray.copy(alpha = 0.5f),
                        disabledTrailingIconContentColor = Color.DarkGray.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // The chart is now conditionally displayed based on the selected period
        val shouldShowChart = when (selectedPeriod) {
            TimePeriod.DAY -> steps > 0
            TimePeriod.WEEK, TimePeriod.MONTH -> true
        }

        if (shouldShowChart) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(top = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Wykres Kroków (${selectedPeriod.label})",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Column(horizontalAlignment = Alignment.Start) {
                        chartData.forEach { data ->
                            Text("${data.label}: ${data.steps} kroków", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

private fun updateChartData(chartData: MutableList<StepData>, currentTotalSteps: Int, period: TimePeriod) {
    if (period != TimePeriod.DAY) {
        // For WEEK and MONTH, we just show placeholder data and don't need the stateful logic.
        chartData.clear()
        if (period == TimePeriod.WEEK) {
            chartData.addAll(listOf(
                StepData("Pon", 5200), StepData("Wto", 8500), StepData("Śro", 10200),
                StepData("Czw", 9100), StepData("Pią", 12000), StepData("Sob", 4500), StepData("Nie", 3100)
            ))
        } else if (period == TimePeriod.MONTH) {
            chartData.addAll(listOf(
                StepData("Tydzień 1", 55000), StepData("Tydzień 2", 75000),
                StepData("Tydzień 3", 62000), StepData("Tydzień 4", 88000)
            ))
        }
        return
    }

    if (period == TimePeriod.DAY && chartData.any { !it.label.contains(":") }) {
        // If we switch to DAY view, clear the old placeholder data
        chartData.clear()
    }

    val calendar = Calendar.getInstance()
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val currentHourLabel = "${currentHour.toString().padStart(2, '0')}:00"

    val totalStepsInChart = chartData.sumOf { it.steps }
    val newStepsSinceLastUpdate = currentTotalSteps - totalStepsInChart

    if (newStepsSinceLastUpdate < 0) {
        // This can happen if the sensor resets (e.g. app restart). Clear and start over.
        chartData.clear()
        chartData.add(StepData(label = currentHourLabel, steps = currentTotalSteps))
        return
    }

    val currentHourData = chartData.find { it.label == currentHourLabel }

    if (currentHourData == null) {
        // New hour, add a new bar
        chartData.add(StepData(label = currentHourLabel, steps = newStepsSinceLastUpdate))
    } else {
        // Same hour, just update the steps for the current bar
        currentHourData.steps += newStepsSinceLastUpdate
    }
}


@Preview
@Composable
fun HomeScreenPreview() {
    ProjektTheme {
        HomeScreen(userName = "Użytkownik", useMetric = true, steps = 12345, stepGoal = 10000)
    }
}