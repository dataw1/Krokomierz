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
import kotlin.math.sqrt

class StepCounter(private val context: Context) {

    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    fun isSensorAvailable(): Boolean {
        val pm = context.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER) ||
                pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)
    }

    val steps: Flow<Int> = callbackFlow {
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor != null) {
            // Use the optimized step counter sensor if available
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.let { launch { send(it.values[0].toInt()) } }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_UI)
            awaitClose { sensorManager.unregisterListener(listener) }
        } else {
            // Fallback to accelerometer for simulation in emulator
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (accelerometer != null) {
                var stepCount = 0
                var lastPeakTime: Long = 0
                var isPeak = false
                val upperThreshold = 11.5f // Adjusted for emulator sensitivity
                val lowerThreshold = 10.5f

                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        if (event == null) return

                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]
                        val magnitude = sqrt(x * x + y * y + z * z)

                        val now = System.currentTimeMillis()
                        if (now - lastPeakTime > 300) { // Debounce
                            if (magnitude > upperThreshold && !isPeak) {
                                isPeak = true
                            }
                            if (magnitude < lowerThreshold && isPeak) {
                                stepCount++
                                launch { send(stepCount) }
                                isPeak = false
                                lastPeakTime = now
                            }
                        }
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }
                sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
                awaitClose { sensorManager.unregisterListener(listener) }
            } else {
                close(IllegalStateException("Brak dostępnego licznika kroków lub akcelerometru"))
            }
        }
    }
}