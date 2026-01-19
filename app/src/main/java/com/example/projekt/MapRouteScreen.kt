package com.example.projekt

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

/**
 * @file MapRouteScreen.kt
 * @brief Ekran mapy umożliwiający planowanie, wyszukiwanie i podążanie trasami.
 */

/**
 * @brief Główny komponent interfejsu mapy.
 * 
 * Obsługuje:
 * - Wyświetlanie mapy Google.
 * - Ręczne dodawanie punktów trasy poprzez kliknięcie.
 * - Wyszukiwanie lokalizacji startowej i docelowej za pomocą Geocoding API.
 * - Wyznaczanie trasy wzdłuż dróg za pomocą Directions API.
 * - Podążanie za wcześniej zapisaną trasą.
 * - Obliczanie i wyświetlanie dystansu.
 *
 * @param modifier Modyfikator układu.
 * @param historyViewModel ViewModel do zarządzania zapisanymi trasami i stanem wybranej trasy.
 */
@SuppressLint("MissingPermission")
@Composable
fun MapRouteScreen(
    modifier: Modifier = Modifier,
    historyViewModel: HistoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiKey = "AIzaSyDxk1kBF2tMwyXR3QEu_TfEERAbiTt4AG8"
    val packageName = "com.example.projekt"
    val certFingerprint = "FE2D764988055998BC251DB7F27E8581148F7C27"

    val historyState by historyViewModel.historyState.collectAsState()
    val selectedRoute = historyState.selectedRouteForFollowing

    val markers = remember { mutableStateListOf<LatLng>() }
    val routePoints = remember { mutableStateListOf<LatLng>() }
    var totalDistanceMeters by remember { mutableLongStateOf(0L) }
    
    var startAddress by remember { mutableStateOf("") }
    var endAddress by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

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

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    /**
     * @brief Aktualizuje trasę na mapie na podstawie aktualnych znaczników (markers).
     * Pobiera dane o przebiegu dróg z Google Directions API.
     */
    fun updateRoute() {
        if (markers.size < 2) {
            routePoints.clear()
            totalDistanceMeters = 0
            return
        }
        scope.launch {
            try {
                val origin = "${markers.first().latitude},${markers.first().longitude}"
                val destination = "${markers.last().latitude},${markers.last().longitude}"
                val waypoints = if (markers.size > 2) {
                    "optimize:false|" + markers.subList(1, markers.size - 1).joinToString("|") { "${it.latitude},${it.longitude}" }
                } else null

                val response = directionsService.getDirections(packageName, certFingerprint, origin, destination, waypoints, "walking", apiKey)
                if (response.status == "OK" && response.routes.isNotEmpty()) {
                    routePoints.clear()
                    routePoints.addAll(PolyUtil.decode(response.routes[0].overviewPolyline.points))
                    totalDistanceMeters = response.routes[0].legs.sumOf { it.distance.value.toLong() }
                }
            } catch (e: Exception) {
                Log.e("MapRoute", "Error: ${e.message}")
            }
        }
    }

    /**
     * @brief Funkcja wyszukująca współrzędne dla podanych adresów i wyznaczająca trasę między nimi.
     * Obsługuje specjalną frazę "moja lokalizacja".
     */
    suspend fun searchAndSetRoute() {
        isSearching = true
        try {
            val startLatLng: LatLng? = if (startAddress.lowercase().trim() == "moja lokalizacja") {
                val location = fusedLocationClient.lastLocation.await()
                location?.let { LatLng(it.latitude, it.longitude) }
            } else {
                val geoResponse = directionsService.geocode(packageName, certFingerprint, startAddress, apiKey)
                if (geoResponse.status == "OK") {
                    val loc = geoResponse.results[0].geometry.location
                    LatLng(loc.lat, loc.lng)
                } else null
            }

            val endGeoResponse = directionsService.geocode(packageName, certFingerprint, endAddress, apiKey)
            val endLatLng: LatLng? = if (endGeoResponse.status == "OK") {
                val loc = endGeoResponse.results[0].geometry.location
                LatLng(loc.lat, loc.lng)
            } else null

            if (startLatLng != null && endLatLng != null) {
                markers.clear()
                markers.add(startLatLng)
                markers.add(endLatLng)
                updateRoute()
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(startLatLng, 14f))
            }
        } catch (e: Exception) {
            Log.e("MapRoute", "Search Error: ${e.message}")
        } finally {
            isSearching = false
        }
    }

    var routeName by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true),
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
                Marker(state = MarkerState(position = latLng), title = if (index == 0) "Start" else "Cel")
            }
        }

        // Panel wyszukiwania
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            OutlinedTextField(
                value = startAddress,
                onValueChange = { startAddress = it },
                label = { Text("Od (np. Moja lokalizacja / Ulica)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = endAddress,
                onValueChange = { endAddress = it },
                label = { Text("Do (Cel podróży)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { scope.launch { searchAndSetRoute() } },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSearching && startAddress.isNotBlank() && endAddress.isNotBlank()
            ) {
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Wyznacz trasę")
                    }
                }
            }
        }

        if (totalDistanceMeters > 0) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp),
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "Dystans: ${String.format(Locale.getDefault(), "%.2f", totalDistanceMeters / 1000.0)} km",
                    color = Color.Cyan,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        if (selectedRoute == null) {
            Card(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = { if (markers.isNotEmpty()) { markers.removeAt(markers.size - 1); updateRoute() } }) { Text("Cofnij") }
                    Button(onClick = { showSaveDialog = true }, enabled = markers.size >= 2) { Text("Zapisz") }
                    TextButton(onClick = { markers.clear(); routePoints.clear(); totalDistanceMeters = 0; startAddress = ""; endAddress = "" }) { Text("Wyczyść", color = Color.Red) }
                }
            }
        } else {
             Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                color = Color(0xFF4CAF50),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Podążasz trasą: ${selectedRoute.name}", color = Color.White)
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = { historyViewModel.selectRouteForFollowing(null); markers.clear(); routePoints.clear() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text("Zakończ")
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Zapisz trasę") },
            text = {
                OutlinedTextField(
                    value = routeName,
                    onValueChange = { routeName = it },
                    label = { Text("Nazwa trasy") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    historyViewModel.saveRoute(
                        routeName.ifBlank { "Trasa ${System.currentTimeMillis()}" },
                        routePoints.map { it.toMyLatLng() },
                        totalDistanceMeters / 1000.0
                    )
                    showSaveDialog = false
                    markers.clear()
                    routePoints.clear()
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
