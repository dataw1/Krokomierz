package com.example.projekt

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.tasks.await

class StepCounterService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var stepCounter: StepCounter
    private lateinit var themePreferenceManager: ThemePreferenceManager
    private val database by lazy { Firebase.database }
    private var userId: String? = null
    private var isTaskRunning = false

    companion object {
        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_CHANNEL_ID = "StepCounterChannel"
    }

    override fun onCreate() {
        super.onCreate()
        stepCounter = StepCounter(this)
        themePreferenceManager = ThemePreferenceManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification("Rozpoczęto liczenie kroków..."))

        intent?.getStringExtra("USER_ID")?.let { newUserId ->
            if (userId != newUserId) {
                userId = newUserId
                serviceScope.launch {
                    themePreferenceManager.setLastUserId(newUserId)
                }
            }
        }

        if (!isTaskRunning) {
            isTaskRunning = true
            serviceScope.launch {
                // Ensure userId is loaded before starting the listener
                userId = userId ?: themePreferenceManager.lastUserId.first()
                if (userId != null) {
                    listenToSteps()
                } else {
                    Log.w("StepCounterService", "User ID is null. Stopping service.")
                    stopSelf()
                }
            }
        }

        return START_STICKY
    }

    private fun listenToSteps() {
        serviceScope.launch {
            var lastSaveTime = 0L
            var initialSteps = -1
            var lastCountDate: String? = null

            stepCounter.steps
                .sample(2000) // Process the latest value every 2 seconds to avoid overload
                .catch { e ->
                    Log.e("StepCounterService", "Step counter sensor not available or failed.", e)
                    stopSelf()
                }
                .collect { totalStepsFromBoot ->
                    try {
                        // Initialize state from DataStore on the first run
                        if (initialSteps == -1) {
                            initialSteps = themePreferenceManager.initialSteps.first()
                            lastCountDate = themePreferenceManager.lastCountDate.first()
                        }

                        val todayDate = themePreferenceManager.getTodayDateString()
                        val deviceRebooted = totalStepsFromBoot < initialSteps

                        val sessionSteps = if (todayDate != lastCountDate || deviceRebooted) {
                            // New day or reboot detected, reset the baseline
                            themePreferenceManager.saveStepCounterSessionState(totalStepsFromBoot, todayDate)
                            initialSteps = totalStepsFromBoot
                            lastCountDate = todayDate
                            0
                        } else {
                            if (initialSteps == 0 && totalStepsFromBoot > 0) {
                                themePreferenceManager.saveStepCounterSessionState(totalStepsFromBoot, todayDate)
                                initialSteps = totalStepsFromBoot
                                0
                            } else {
                                totalStepsFromBoot - initialSteps
                            }
                        }

                        themePreferenceManager.updateUiSteps(sessionSteps)
                        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(NOTIFICATION_ID, createNotification("Dzisiejsze kroki: $sessionSteps"))

                        // --- Firebase Sync Logic ---
                        val currentTime = System.currentTimeMillis()
                        val lastSavedDateFirebase = themePreferenceManager.dateForSavedSteps.first()
                        val stepsAlreadySavedToday = if (todayDate == lastSavedDateFirebase) themePreferenceManager.stepsSavedOnDate.first() else 0

                        val stepsSinceLastSave = sessionSteps - stepsAlreadySavedToday
                        val currentUserId = userId

                        if (currentUserId != null && (stepsSinceLastSave >= 20 || (stepsSinceLastSave > 0 && currentTime - lastSaveTime > 300_000))) {
                            val userName = themePreferenceManager.userName.first() ?: "Użytkownik"

                            val activityData = UserActivityData(
                                name = userName,
                                steps = stepsSinceLastSave,
                                distance = (stepsSinceLastSave * 80) / 100_000.0,
                                calories = stepsSinceLastSave * 0.04f,
                                timestamp = ServerValue.TIMESTAMP
                            )
                            database.getReference("userActivity").child(currentUserId).push().setValue(activityData).await()
                            themePreferenceManager.updateFirebaseSavedSteps(todayDate, sessionSteps)
                            lastSaveTime = currentTime
                        }
                    } catch (e: Exception) {
                        Log.e("StepCounterService", "Error while processing step data.", e)
                    }
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(text: String): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "Licznik Kroków", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Aktywność")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use a system icon to avoid resource not found issues
            .setOnlyAlertOnce(true)
            .setOngoing(true) // Make the notification persistent
            .build()
    }
}