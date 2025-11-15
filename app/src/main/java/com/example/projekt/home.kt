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
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.material3.ChipColors
import androidx.compose.ui.graphics.Color

// Enumerator do zarządzania widokiem wykresu
enum class TimePeriod {
    DAY,
    WEEK,
    MONTH
}

// Model danych dla wykresu (uproszczony)
data class StepData(val label: String, val steps: Int)

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    // Stan przechowujący aktualnie wybrany zakres czasu
    var selectedPeriod by remember { mutableStateOf(TimePeriod.DAY) }

    // Dane demonstracyjne zależne od wybranego okresu
    val currentStepCount = 8542
    val goal = 10000

    // Modelowanie danych dla wykresu
    val chartData = getDemoDataForPeriod(selectedPeriod)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 1. GŁÓWNY LICZNIK KROKÓW (Wskaźnik Postępu)
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
                    text = currentStepCount.toString(),
                    fontSize = 52.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("Cel: $goal kroków", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = currentStepCount / goal.toFloat(),
                    modifier = Modifier.fillMaxWidth().height(12.dp),
                    color = if (currentStepCount >= goal) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                )
            }
        }

        // 2. WYBÓR OKRESU (Chips)
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
                    label = { Text(period.name.lowercase().capitalize(Locale.current)) },
                    // Użycie ChipColors do rozwiązania problemu ChipDefaults
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

        // 3. WYKRES KROKÓW (Placeholder)
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
                    text = "Wykres Kroków (${selectedPeriod.name.capitalize(Locale.current)})",
                    style = MaterialTheme.typography.headlineSmall
                )

                // MIEJSCE NA WYKRES: Na razie wyświetlamy dane tekstowo
                Column(horizontalAlignment = Alignment.Start) {
                    chartData.forEach { data ->
                        Text("${data.label}: ${data.steps} kroków", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

// Funkcja zwracająca dane demonstracyjne
private fun getDemoDataForPeriod(period: TimePeriod): List<StepData> {
    return when (period) {
        TimePeriod.DAY -> listOf(
            StepData("00-04", 150),
            StepData("04-08", 1200),
            StepData("08-12", 3000),
            StepData("12-16", 4500),
            StepData("16-20", 2500),
            StepData("20-24", 500)
        )
        TimePeriod.WEEK -> listOf(
            StepData("Pon", 5200),
            StepData("Wto", 8500),
            StepData("Śro", 10200),
            StepData("Czw", 9100),
            StepData("Pią", 12000),
            StepData("Sob", 4500),
            StepData("Nie", 3100)
        )
        TimePeriod.MONTH -> listOf(
            StepData("Tydz 1", 55000),
            StepData("Tydz 2", 75000),
            StepData("Tydz 3", 62000),
            StepData("Tydz 4", 88000)
        )
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    ProjektTheme {
        HomeScreen()
    }
}