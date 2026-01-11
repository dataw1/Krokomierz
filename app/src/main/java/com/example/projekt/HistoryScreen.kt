package com.example.projekt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    historyViewModel: HistoryViewModel = viewModel(),
    onFollowRoute: () -> Unit = {}
) {
    val historyState by historyViewModel.historyState.collectAsState()
    var routeToDelete by remember { mutableStateOf<RouteData?>(null) }

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
            Text("Moje trasy", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))

            if (historyState.routes.isEmpty()) {
                Text("Brak zapisanych tras.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(historyState.routes) { route ->
                        RouteItem(
                            route = route,
                            onDelete = { routeToDelete = route },
                            onFollow = {
                                historyViewModel.selectRouteForFollowing(route)
                                onFollowRoute()
                            }
                        )
                    }
                }
            }
        }
    }

    if (routeToDelete != null) {
        AlertDialog(
            onDismissRequest = { routeToDelete = null },
            title = { Text("Usuń trasę") },
            text = { Text("Czy na pewno chcesz usunąć trasę \"${routeToDelete?.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        routeToDelete?.let { historyViewModel.deleteRoute(it.id) }
                        routeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Usuń")
                }
            },
            dismissButton = {
                TextButton(onClick = { routeToDelete = null }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

@Composable
fun RouteItem(route: RouteData, onDelete: () -> Unit, onFollow: () -> Unit) {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val dateString = sdf.format(Date(route.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = route.name, style = MaterialTheme.typography.titleMedium)
                    Text(text = dateString, style = MaterialTheme.typography.bodySmall)
                }
                Row {
                    IconButton(onClick = onFollow) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Podążaj", tint = Color(0xFF4CAF50))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Dystans: ${String.format("%.2f", route.distanceKm)} km", style = MaterialTheme.typography.bodyMedium)
                Text(text = "${route.points.size} pkt", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
