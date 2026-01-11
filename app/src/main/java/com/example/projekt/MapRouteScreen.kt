package com.example.projekt

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.util.Locale

@Composable
fun MapRouteScreen(
    modifier: Modifier = Modifier,
    historyViewModel: HistoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val points = remember { mutableStateListOf<LatLng>() }
    
    // Warszawa jako punkt startowy
    val initialPos = LatLng(52.2297, 21.0122)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPos, 12f)
    }

    val hasLocationPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    var routeName by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission.value,
                mapType = MapType.NORMAL,
                isTrafficEnabled = false
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = true,
                zoomControlsEnabled = true,
                compassEnabled = true,
                mapToolbarEnabled = true
            ),
            onMapClick = { latLng ->
                points.add(latLng)
            }
        ) {
            if (points.isNotEmpty()) {
                Polyline(
                    points = points.toList(),
                    color = Color(0xFF2196F3), // Jasny niebieski
                    width = 15f
                )
                points.forEachIndexed { index, latLng ->
                    Marker(
                        state = MarkerState(position = latLng),
                        title = if (index == 0) "Start" else if (index == points.size - 1) "Meta" else "Punkt ${index + 1}",
                        snippet = "Kliknij, aby usunąć",
                        onClick = {
                            points.remove(latLng)
                            true
                        }
                    )
                }
            }
        }

        // Informacja pomocnicza
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            color = Color.Black.copy(alpha = 0.7f),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = if (points.isEmpty()) "Dotknij mapy, aby wyznaczyć trasę" else "Punkty: ${points.size}",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Panel dolny
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Dystans", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.2f", calculateDistance(points))} km",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                
                Row {
                    if (points.isNotEmpty()) {
                        IconButton(onClick = { points.clear() }) {
                            Text("X", color = Color.Red)
                        }
                    }
                    Button(
                        onClick = { showSaveDialog = true },
                        enabled = points.size >= 2
                    ) {
                        Text("Zapisz")
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Nowa trasa") },
            text = {
                OutlinedTextField(
                    value = routeName,
                    onValueChange = { routeName = it },
                    label = { Text("Nazwa trasy") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    historyViewModel.saveRoute(
                        routeName.ifBlank { "Trasa ${System.currentTimeMillis()}" },
                        points.map { it.toMyLatLng() },
                        calculateDistance(points)
                    )
                    showSaveDialog = false
                    points.clear()
                    routeName = ""
                }) {
                    Text("Zapisz")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

private fun calculateDistance(points: List<LatLng>): Double {
    if (points.size < 2) return 0.0
    var totalDistance = 0.0
    for (i in 0 until points.size - 1) {
        val results = FloatArray(1)
        Location.distanceBetween(
            points[i].latitude, points[i].longitude,
            points[i+1].latitude, points[i+1].longitude,
            results
        )
        totalDistance += results[0]
    }
    return totalDistance / 1000.0
}
