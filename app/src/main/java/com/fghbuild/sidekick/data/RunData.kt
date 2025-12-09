// Represents all metrics collected during an active run including distance, pace, duration, route points, and heart rate/pace history.
package com.fghbuild.sidekick.data

data class RunData(
    val distanceMeters: Double = 0.0,
    val paceMinPerKm: Double = 0.0,
    val durationMillis: Long = 0L,
    val routePoints: List<RoutePoint> = emptyList(),
    val filteredRoutePoints: List<RoutePoint> = emptyList(),
    val paceHistory: List<PaceWithTime> = emptyList(),
    val heartRateHistory: List<HeartRateWithTime> = emptyList(),
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
)
