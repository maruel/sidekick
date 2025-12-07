package com.fghbuild.sidekick.data

data class RunData(
    val distanceMeters: Double = 0.0,
    val paceMinPerKm: Double = 0.0,
    val durationMillis: Long = 0L,
    val routePoints: List<RoutePoint> = emptyList(),
    val paceHistory: List<PaceWithTime> = emptyList(),
    val heartRateHistory: List<HeartRateWithTime> = emptyList(),
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
)
