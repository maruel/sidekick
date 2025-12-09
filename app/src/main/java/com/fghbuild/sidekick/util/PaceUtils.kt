// Calculates running pace metrics and defines training zones.
// Converts duration and distance to pace (min/km), determines pace zones,
// and formats pace and duration for display.
package com.fghbuild.sidekick.util

import androidx.compose.ui.graphics.Color
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

    // Generic selector functions to reduce duplication
    fun <T> averageOrDefault(
        items: List<T>,
        selector: (T) -> Double,
        default: Double = 0.0,
    ): Double {
        return if (items.isEmpty()) default else items.map { selector(it) }.average()
    }

    fun <T> maxOrDefault(
        items: List<T>,
        selector: (T) -> Double,
        default: Double = 0.0,
    ): Double {
        return items.mapNotNull { selector(it) }.maxOrNull() ?: default
    }

    fun <T> minOrDefault(
        items: List<T>,
        selector: (T) -> Double,
        default: Double = 0.0,
    ): Double {
        return items.mapNotNull { selector(it) }.minOrNull() ?: default
    }

    // Keep legacy functions for backward compatibility
    fun calculateAveragePace(paceHistory: List<Double>): Double {
        return averageOrDefault(paceHistory, { it })
    }

    fun calculateMaxPace(paceHistory: List<Double>): Double {
        return maxOrDefault(paceHistory, { it })
    }

    fun calculateMinPace(paceHistory: List<Double>): Double {
        return minOrDefault(paceHistory, { it })
    }

    fun calculateAveragePaceWithTime(paceHistory: List<PaceWithTime>): Double {
        return averageOrDefault(paceHistory, { it.pace })
    }

    fun calculateMaxPaceWithTime(paceHistory: List<PaceWithTime>): Double {
        return maxOrDefault(paceHistory, { it.pace })
    }

    fun calculateMinPaceWithTime(paceHistory: List<PaceWithTime>): Double {
        return minOrDefault(paceHistory, { it.pace })
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

    fun getZoneColor(zone: Int): Color {
        return when (zone) {
            1 -> Color(0xFFF44336) // Red - Recovery (slowest)
            2 -> Color(0xFFFF9800) // Orange - Easy
            3 -> Color(0xFFFFC107) // Yellow - Moderate
            4 -> Color(0xFF8BC34A) // Light Green - Tempo
            5 -> Color(0xFF4CAF50) // Green - Fast (fastest)
            else -> Color.Gray
        }
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
