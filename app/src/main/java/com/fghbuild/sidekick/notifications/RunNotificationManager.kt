package com.fghbuild.sidekick.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.fghbuild.sidekick.R

class RunNotificationManager(private val context: Context) {
    companion object {
        private const val CHANNEL_ID = "sidekick_run_channel"
        private const val NOTIFICATION_ID = 1
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Run Tracking",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            channel.description = "Notifications for active run tracking"
            channel.setSound(null, null)
            channel.enableVibration(false)
            channel.enableLights(false)
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun updateNotification(
        distanceKm: Double,
        paceMinPerKm: Double,
        durationSeconds: Long,
    ): Notification {
        val pace = formatPace(paceMinPerKm)
        val duration = formatDuration(durationSeconds)
        val text = "${"%.2f".format(distanceKm)} km • $pace min/km • $duration"

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Run in Progress")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(text),
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    fun getNotificationId(): Int = NOTIFICATION_ID

    private fun formatPace(paceMinPerKm: Double): String {
        return if (paceMinPerKm.isFinite() && paceMinPerKm > 0) {
            val minutes = paceMinPerKm.toInt()
            val seconds = ((paceMinPerKm - minutes) * 60).toInt()
            "$minutes:${"%02d".format(seconds)}"
        } else {
            "0:00"
        }
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return "${"%02d".format(hours)}:${"%02d".format(minutes)}:${"%02d".format(secs)}"
    }
}
