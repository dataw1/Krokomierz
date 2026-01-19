/**
 * @file GyroscopeManager.kt
 * @brief Klasa zarządzająca odczytami z czujnika żyroskopu.
 */

package com.example.projekt

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * @struct GyroscopeData
 * @brief Struktura przechowująca prędkość kątową w trzech osiach.
 * 
 * @property x Prędkość obrotowa wokół osi X (rad/s).
 * @property y Prędkość obrotowa wokół osi Y (rad/s).
 * @property z Prędkość obrotowa wokół osi Z (rad/s).
 */
data class GyroscopeData(val x: Float, val y: Float, val z: Float)

/**
 * @class GyroscopeManager
 * @brief Menedżer czujnika żyroskopu.
 * 
 * Klasa zapewnia dostęp do danych z żyroskopu za pomocą mechanizmu Flow.
 * Umożliwia sprawdzenie dostępności czujnika oraz ciągłe monitorowanie obrotów urządzenia.
 */
class GyroscopeManager(private val context: Context) {

    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    /**
     * @brief Sprawdza, czy urządzenie posiada fizyczny czujnik żyroskopu.
     * @return true jeśli żyroskop jest dostępny, false w przeciwnym razie.
     */
    fun isGyroscopeAvailable(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE)
    }

    /**
     * @brief Strumień danych (Flow) emitujący aktualne odczyty z żyroskopu.
     * 
     * Rejestruje słuchacza zdarzeń czujnika i wysyła dane typu [GyroscopeData].
     * Zwalnia zasoby (wyrejestrowuje słuchacza) przy zamknięciu strumienia.
     */
    val rotationData: Flow<GyroscopeData> = callbackFlow {
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        if (gyroscope == null) {
            close(IllegalStateException("Czujnik żyroskopu jest niedostępny"))
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val data = GyroscopeData(it.values[0], it.values[1], it.values[2])
                    launch { send(data) }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, gyroscope, SensorManager.SENSOR_DELAY_NORMAL)

        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
