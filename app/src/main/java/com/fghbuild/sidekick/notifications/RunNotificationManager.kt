package com.fghbuild.sidekick.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.fghbuild.sidekick.MainActivity
import com.fghbuild.sidekick.R
import com.fghbuild.sidekick.util.PaceUtils

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
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            channel.description = context.getString(R.string.notification_channel_description)
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
        currentBpm: Int = 0,
    ): Notification {
        val pace = PaceUtils.formatPace(paceMinPerKm)
        val duration = formatDuration(durationSeconds)
        val text =
            if (currentBpm > 0) {
                context.getString(
                    R.string.notification_text_with_bpm,
                    distanceKm,
                    pace,
                    duration,
                    currentBpm,
                )
            } else {
                context.getString(R.string.notification_text, distanceKm, pace, duration)
            }

        val intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_title))
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
            .setContentIntent(pendingIntent)
            .build()
    }

    fun getNotificationId(): Int = NOTIFICATION_ID

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return "${"%02d".format(hours)}:${"%02d".format(minutes)}:${"%02d".format(secs)}"
    }
}
