package com.example.projekt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    historyViewModel: HistoryViewModel = viewModel()
) {
    val historyState by historyViewModel.historyState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (historyState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (historyState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Błąd: ${historyState.error}",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            Text("Statystyki kroków", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ostatnie 7 dni: ${historyState.dailyStepsLastWeek.values.sum()} kroków")
                    Text("Ostatnie 30 dni: ${historyState.weeklyStepsLastMonth.values.sum()} kroków")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Zapisane trasy", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))

            if (historyState.routes.isEmpty()) {
                Text("Brak zapisanych tras.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(historyState.routes) { route ->
                        RouteItem(route)
                    }
                }
            }
        }
    }
}

@Composable
fun RouteItem(route: RouteData) {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val dateString = sdf.format(Date(route.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = route.name, style = MaterialTheme.typography.titleMedium)
            Text(text = "Data: $dateString", style = MaterialTheme.typography.bodySmall)
            Text(text = "Dystans: ${String.format("%.2f", route.distanceKm)} km", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Liczba punktów: ${route.points.size}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
