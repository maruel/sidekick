package com.fghbuild.sidekick.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.fghbuild.sidekick.notifications.RunNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

class RunTrackingService : Service() {
    private val binder = LocalBinder()
    private lateinit var notificationManager: RunNotificationManager
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var isInForeground = false
    private var lastNotificationUpdateTimeMillis = 0L

    inner class LocalBinder : Binder() {
        fun getService(): RunTrackingService = this@RunTrackingService
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = RunNotificationManager(this)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        return when (intent?.action) {
            ACTION_START_RUN -> {
                startForegroundTracking()
                START_STICKY
            }

            ACTION_STOP_RUN -> {
                stopForegroundTracking()
                stopSelf()
                START_NOT_STICKY
            }

            else -> START_STICKY
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun isRunActive(): Boolean = isInForeground

    fun updateNotification(
        distanceKm: Double,
        paceMinPerKm: Double,
        durationSeconds: Long,
        currentBpm: Int,
    ) {
        if (isInForeground) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastNotificationUpdateTimeMillis >= NOTIFICATION_UPDATE_THROTTLE_MS) {
                val notification =
                    notificationManager.updateNotification(
                        distanceKm = distanceKm,
                        paceMinPerKm = paceMinPerKm,
                        durationSeconds = durationSeconds,
                        currentBpm = currentBpm,
                    )
                updateForegroundNotification(notification)
                lastNotificationUpdateTimeMillis = currentTime
            }
        }
    }

    private fun startForegroundTracking() {
        if (!isInForeground) {
            isInForeground = true
            val notification = notificationManager.updateNotification(0.0, 0.0, 0L, 0)
            startForeground(notificationManager.getNotificationId(), notification)
        }
    }

    private fun updateForegroundNotification(notification: Notification) {
        startForeground(notificationManager.getNotificationId(), notification)
    }

    private fun stopForegroundTracking() {
        if (isInForeground) {
            isInForeground = false
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForegroundTracking()
        scope.cancel()
    }

    companion object {
        const val ACTION_START_RUN = "com.fghbuild.sidekick.ACTION_START_RUN"
        const val ACTION_STOP_RUN = "com.fghbuild.sidekick.ACTION_STOP_RUN"
        private const val NOTIFICATION_UPDATE_THROTTLE_MS = 2000L // Update at most every 2 seconds
    }
}
