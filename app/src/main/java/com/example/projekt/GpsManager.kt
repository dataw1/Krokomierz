/**
 * @file GpsManager.kt
 * @brief Klasa zarządzająca odczytami GPS i obliczaniem przebytego dystansu.
 */

package com.example.projekt

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * @class GpsManager
 * @brief Menedżer lokalizacji GPS.
 * 
 * Klasa wykorzystuje FusedLocationProviderClient do śledzenia pozycji użytkownika
 * w czasie rzeczywistym i obliczania całkowitego przebytego dystansu w metrach.
 */
class GpsManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * @brief Tworzy strumień (Flow) danych o przebytym dystansie.
     * 
     * Rejestruje żądania lokalizacji o wysokiej dokładności i oblicza dystans
     * między kolejnymi punktami geograficznymi.
     * 
     * @return Flow emitujący skumulowany dystans w metrach (Float).
     */
    @SuppressLint("MissingPermission")
    fun getDistanceFlow(): Flow<Float> = callbackFlow {
        var totalDistance = 0f
        var lastLocation: Location? = null

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    lastLocation?.let {
                        val distance = it.distanceTo(location)
                        if (distance > 1.0) { // Tylko jeśli przesunięcie jest znaczące (np. > 1m)
                            totalDistance += distance
                            launch { send(totalDistance) }
                        }
                    }
                    lastLocation = location
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        /**
         * @brief Zatrzymuje aktualizacje lokalizacji przy zamknięciu strumienia.
         */
        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}
