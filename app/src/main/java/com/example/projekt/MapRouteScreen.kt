package com.example.projekt

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
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
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

@Composable
fun MapRouteScreen(
    modifier: Modifier = Modifier,
    historyViewModel: HistoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiKey = "AIzaSyDxk1kBF2tMwyXR3QEu_TfEERAbiTt4AG8"
    
    // Dane do autoryzacji nagłówków (pobrane z Twojego błędu Logcat)
    val packageName = "com.example.projekt"
    val certFingerprint = "FE2D764988055998BC251DB7F27E8581148F7C27" // Bez dwukropków

    val historyState by historyViewModel.historyState.collectAsState()
    val selectedRoute = historyState.selectedRouteForFollowing

    val markers = remember { mutableStateListOf<LatLng>() }
    val routePoints = remember { mutableStateListOf<LatLng>() }
    var totalDistanceMeters by remember { mutableLongStateOf(0L) }
    var apiStatusInfo by remember { mutableStateOf<String?>(null) }
    var fullErrorMsg by remember { mutableStateOf<String?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(52.2297, 21.0122), 12f)
    }

    val directionsService = remember {
        Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DirectionsService::class.java)
    }

    fun calculateStraightDistance(points: List<LatLng>): Double {
        var dist = 0.0
        for (i in 0 until points.size - 1) {
            val results = FloatArray(1)
            Location.distanceBetween(points[i].latitude, points[i].longitude, points[i+1].latitude, points[i+1].longitude, results)
            dist += results[0]
        }
        return dist
    }

    fun updateRoute() {
        if (markers.size < 2) {
            routePoints.clear()
            totalDistanceMeters = 0
            apiStatusInfo = null
            fullErrorMsg = null
            return
        }

        scope.launch {
            try {
                val origin = "${markers.first().latitude},${markers.first().longitude}"
                val destination = "${markers.last().latitude},${markers.last().longitude}"
                val waypoints = if (markers.size > 2) {
                    "optimize:false|" + markers.subList(1, markers.size - 1).joinToString("|") { "${it.latitude},${it.longitude}" }
                } else null

                val response = directionsService.getDirections(
                    packageName = packageName,
                    certFingerprint = certFingerprint,
                    origin = origin,
                    destination = destination,
                    waypoints = waypoints,
                    mode = "walking",
                    apiKey = apiKey
                )

                if (response.status == "OK" && response.routes.isNotEmpty()) {
                    apiStatusInfo = "OK (Trasa drogowa)"
                    fullErrorMsg = null
                    val polyline = response.routes[0].overviewPolyline.points
                    routePoints.clear()
                    routePoints.addAll(PolyUtil.decode(polyline))
                    totalDistanceMeters = response.routes[0].legs.sumOf { it.distance.value.toLong() }
                } else {
                    apiStatusInfo = "Status: ${response.status}"
                    fullErrorMsg = response.errorMessage ?: "Brak szczegółów błędu"
                    routePoints.clear()
                    routePoints.addAll(markers)
                    totalDistanceMeters = calculateStraightDistance(markers).toLong()
                }
            } catch (e: Exception) {
                apiStatusInfo = "Błąd połączenia"
                fullErrorMsg = e.message
                routePoints.clear()
                routePoints.addAll(markers)
                totalDistanceMeters = calculateStraightDistance(markers).toLong()
            }
        }
    }

    LaunchedEffect(selectedRoute) {
        if (selectedRoute != null) {
            markers.clear()
            if (selectedRoute.points.isNotEmpty()) {
                markers.add(selectedRoute.points.first().toLatLng())
                if (selectedRoute.points.size > 1) markers.add(selectedRoute.points.last().toLatLng())
            }
            routePoints.clear()
            routePoints.addAll(selectedRoute.points.map { it.toLatLng() })
            totalDistanceMeters = (selectedRoute.distanceKm * 1000).toLong()
            cameraPositionState.position = CameraPosition.fromLatLngZoom(routePoints.first(), 15f)
        }
    }

    val hasLocationPermission = remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    var routeName by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission.value),
            uiSettings = MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = true),
            onMapClick = { latLng ->
                if (selectedRoute == null) {
                    markers.add(latLng)
                    updateRoute()
                }
            }
        ) {
            if (routePoints.isNotEmpty()) {
                Polyline(points = routePoints.toList(), color = if (selectedRoute != null) Color(0xFF4CAF50) else Color(0xFF2196F3), width = 12f)
            }
            markers.forEachIndexed { index, latLng ->
                Marker(state = MarkerState(position = latLng), title = if (index == 0) "Start" else if (index == markers.size - 1) "Koniec" else "Punkt ${index + 1}")
            }
        }

        Surface(modifier = Modifier.align(Alignment.TopCenter).padding(16.dp), color = Color.Black.copy(alpha = 0.8f), shape = MaterialTheme.shapes.medium) {
            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (selectedRoute != null) {
                    Text(text = "Podążasz trasą: ${selectedRoute.name}", color = Color.White)
                    Button(onClick = { historyViewModel.selectRouteForFollowing(null); markers.clear(); routePoints.clear() }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("Zakończ") }
                } else {
                    apiStatusInfo?.let {
                        Text(text = it, color = if (it.startsWith("OK")) Color.Green else Color.Yellow, style = MaterialTheme.typography.titleSmall)
                    }
                    fullErrorMsg?.let {
                        Text(text = it, color = Color.Red, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                    Text(text = "Dystans: ${String.format(Locale.getDefault(), "%.2f", totalDistanceMeters / 1000.0)} km", color = Color.Cyan, style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        if (selectedRoute == null) {
            Card(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth()) {
                Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = { if (markers.isNotEmpty()) { markers.removeAt(markers.size - 1); updateRoute() } }) { Text("Cofnij") }
                    Button(onClick = { showSaveDialog = true }, enabled = markers.size >= 2) { Text("Zapisz") }
                    TextButton(onClick = { markers.clear(); routePoints.clear(); totalDistanceMeters = 0 }) { Text("Wyczyść", color = Color.Red) }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Zapisz trasę") },
            text = { OutlinedTextField(value = routeName, onValueChange = { routeName = it }, label = { Text("Nazwa trasy") }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Button(onClick = {
                    historyViewModel.saveRoute(routeName.ifBlank { "Trasa ${System.currentTimeMillis()}" }, routePoints.map { it.toMyLatLng() }, totalDistanceMeters / 1000.0)
                    showSaveDialog = false; markers.clear(); routePoints.clear(); routeName = ""
                }) { Text("Zapisz") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Anuluj") } }
        )
    }
}
