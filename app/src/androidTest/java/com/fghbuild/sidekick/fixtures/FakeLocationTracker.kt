package com.fghbuild.sidekick.fixtures

import android.location.Location
import com.fghbuild.sidekick.data.RoutePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake location tracker for testing.
 * Allows tests to feed predetermined GPS points instead of using real location services.
 */
class FakeLocationTracker {
    private val _currentLocation = MutableStateFlow<Location?>(null)
    private val _routePoints = MutableStateFlow<List<RoutePoint>>(emptyList())

    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()
    val routePoints: StateFlow<List<RoutePoint>> = _routePoints.asStateFlow()

    fun startTracking() {
        // No-op for fake
    }

    fun stopTracking() {
        // No-op for fake
    }

    fun resetRoute() {
        _routePoints.value = emptyList()
    }

    /**
     * Feed test route points.
     * Simulates receiving GPS updates.
     */
    fun feedRoutePoints(routePoints: List<RoutePoint>) {
        _routePoints.value = routePoints
        if (routePoints.isNotEmpty()) {
            val lastPoint = routePoints.last()
            _currentLocation.value =
                Location("test").apply {
                    latitude = lastPoint.latitude
                    longitude = lastPoint.longitude
                    time = lastPoint.timestamp
                }
        }
    }

    /**
     * Feed a single route point update.
     */
    fun feedRoutePoint(routePoint: RoutePoint) {
        val location =
            Location("test").apply {
                latitude = routePoint.latitude
                longitude = routePoint.longitude
                time = routePoint.timestamp
            }
        _currentLocation.value = location
        _routePoints.value = _routePoints.value + routePoint
    }

    /**
     * Clear all accumulated route points.
     */
    fun reset() {
        _currentLocation.value = null
        _routePoints.value = emptyList()
    }
}
