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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class StepCounter(private val context: Context) {

    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private val themePreferenceManager = ThemePreferenceManager(context)

    fun isSensorAvailable(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)
    }

    val steps: Flow<Int> = callbackFlow {
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor == null) {
            close(IllegalStateException("Brak dostępnego licznika kroków."))
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            private var initialSteps = -1
            private var todaySteps = 0

            override fun onSensorChanged(event: SensorEvent?) {
                event?.let { sensorEvent ->
                    val totalSteps = sensorEvent.values[0].toInt()

                    launch {
                        val todayDate = themePreferenceManager.getTodayDateString()
                        val lastCountDate = themePreferenceManager.lastCountDate.first()
                        val storedInitialSteps = themePreferenceManager.initialSteps.first()
                        val previousDaySteps = themePreferenceManager.previousDaySteps.first()

                        if (todayDate != lastCountDate) { // New day
                            initialSteps = totalSteps
                            todaySteps = 0
                            themePreferenceManager.saveStepCounterState(totalSteps, todayDate, todaySteps)
                        } else { // Same day
                            if (storedInitialSteps == 0) { // First launch of the day
                                initialSteps = totalSteps
                                themePreferenceManager.saveStepCounterState(totalSteps, todayDate, 0)
                            } else {
                                initialSteps = storedInitialSteps
                            }
                            todaySteps = totalSteps - initialSteps + previousDaySteps
                        }
                        send(todaySteps)
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}