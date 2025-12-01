package com.fghbuild.sidekick.util

object PaceUtils {
    fun calculatePaceMinPerKm(
        durationMillis: Long,
        distanceMeters: Double,
    ): Double {
        if (distanceMeters <= 0) return 0.0
        val durationMinutes = durationMillis / 1000.0 / 60.0
        val distanceKm = distanceMeters / 1000.0
        return durationMinutes / distanceKm
    }

    fun formatPace(paceMinPerKm: Double): String {
        return if (paceMinPerKm.isFinite() && paceMinPerKm > 0) {
            val minutes = paceMinPerKm.toInt()
            val seconds = ((paceMinPerKm - minutes) * 60).toInt()
            String.format("%d:%02d", minutes, seconds)
        } else {
            "0:00"
        }
    }

    fun formatDuration(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}
