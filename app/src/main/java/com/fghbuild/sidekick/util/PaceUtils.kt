package com.fghbuild.sidekick.util

import com.fghbuild.sidekick.data.PaceWithTime

data class PaceZone(
    val zone: Int,
    val name: String,
    val minPace: Double,
    val maxPace: Double,
    val description: String,
)

object PaceUtils {
    fun calculatePaceMinPerKm(
        durationMillis: Long,
        distanceMeters: Double,
    ): Double {
        if (distanceMeters <= 0.0) return 0.0
        val distanceKm = distanceMeters / 1000.0
        val durationMinutes = durationMillis / 60000.0
        return durationMinutes / distanceKm
    }

    fun calculateAveragePace(paceHistory: List<Double>): Double {
        if (paceHistory.isEmpty()) return 0.0
        return paceHistory.average()
    }

    fun calculateMaxPace(paceHistory: List<Double>): Double {
        return paceHistory.maxOrNull() ?: 0.0
    }

    fun calculateMinPace(paceHistory: List<Double>): Double {
        return paceHistory.minOrNull() ?: 0.0
    }

    fun calculateAveragePaceWithTime(paceHistory: List<PaceWithTime>): Double {
        if (paceHistory.isEmpty()) return 0.0
        return paceHistory.map { it.pace }.average()
    }

    fun calculateMaxPaceWithTime(paceHistory: List<PaceWithTime>): Double {
        return paceHistory.map { it.pace }.maxOrNull() ?: 0.0
    }

    fun calculateMinPaceWithTime(paceHistory: List<PaceWithTime>): Double {
        return paceHistory.map { it.pace }.minOrNull() ?: 0.0
    }

    fun getPaceZones(): List<PaceZone> {
        return listOf(
            PaceZone(
                zone = 1,
                name = "Zone 1: Recovery",
                minPace = 6.0,
                maxPace = 10.0,
                description = "Very easy recovery runs",
            ),
            PaceZone(
                zone = 2,
                name = "Zone 2: Easy",
                minPace = 5.0,
                maxPace = 6.0,
                description = "Comfortable aerobic base",
            ),
            PaceZone(
                zone = 3,
                name = "Zone 3: Moderate",
                minPace = 4.0,
                maxPace = 5.0,
                description = "Sustainable effort",
            ),
            PaceZone(
                zone = 4,
                name = "Zone 4: Tempo",
                minPace = 3.0,
                maxPace = 4.0,
                description = "Hard sustainable effort",
            ),
            PaceZone(
                zone = 5,
                name = "Zone 5: Fast",
                minPace = 0.0,
                maxPace = 3.0,
                description = "Maximum effort",
            ),
        )
    }

    fun getZoneForPace(pace: Double): PaceZone? {
        return getPaceZones().find { pace in it.maxPace..it.minPace }
    }

    const val GRAPH_DISPLAY_MIN: Double = 10.5 // Slowest pace to display (10.5 min/km)
    const val GRAPH_DISPLAY_MAX: Double = 1.5 // Fastest pace to display (1.5 min/km)

    fun formatPace(paceMinPerKm: Double): String {
        if (paceMinPerKm <= 0 || !paceMinPerKm.isFinite() || paceMinPerKm > 30.0) {
            return "--"
        }
        val minutes = paceMinPerKm.toInt()
        val seconds = ((paceMinPerKm - minutes) * 60).toInt()
        return "$minutes:${seconds.toString().padStart(2, '0')}"
    }

    fun formatDuration(durationMillis: Long): String {
        val seconds = (durationMillis / 1000) % 60
        val minutes = (durationMillis / (1000 * 60)) % 60
        val hours = durationMillis / (1000 * 60 * 60)
        return "%d:%02d:%02d".format(hours, minutes, seconds)
    }
}
