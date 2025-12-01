package com.fghbuild.sidekick.data

data class RunData(
    val distanceMeters: Double = 0.0,
    val paceMinPerKm: Double = 0.0,
    val durationMillis: Long = 0L,
    val routePoints: List<RoutePoint> = emptyList(),
    val isRunning: Boolean = false,
)
