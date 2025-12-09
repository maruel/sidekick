// Represents a GPS location point with latitude, longitude, timestamp, and optional accuracy, bearing, and speed.
package com.fghbuild.sidekick.data

data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val accuracy: Float = 0.0f,
    val bearing: Float = 0.0f,
    val speed: Float = 0.0f,
)
