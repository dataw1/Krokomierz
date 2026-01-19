/**
 * @file StepCounter.kt
 * @brief Klasa obsługująca sprzętowy czujnik kroków.
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

/**
 * @class StepCounter
 * @brief Menedżer sprzętowego licznika kroków.
 * 
 * Klasa zapewnia dostęp do systemowego czujnika TYPE_STEP_COUNTER.
 * Emituje surowe dane o liczbie kroków od ostatniego uruchomienia urządzenia.
 */
class StepCounter(private val context: Context) {

    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    /**
     * @brief Sprawdza, czy urządzenie posiada sprzętowy licznik kroków.
     * @return true jeśli czujnik jest dostępny, false w przeciwnym razie.
     */
    fun isSensorAvailable(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)
    }

    /**
     * @brief Strumień danych (Flow) emitujący surową liczbę kroków.
     * 
     * Wykorzystuje [callbackFlow] do rejestracji SensorEventListener.
     * Wartość emitowana to liczba kroków od bootowania systemu.
     * 
     * @return Flow<Int> emitujący aktualny stan licznika kroków.
     */
    val steps: Flow<Int> = callbackFlow {
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor == null) {
            close(IllegalStateException("Brak dostępnego licznika kroków."))
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    // Send the raw sensor value
                    trySend(it.values[0].toInt())
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
