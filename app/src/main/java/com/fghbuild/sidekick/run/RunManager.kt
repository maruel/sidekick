package com.fghbuild.sidekick.run

import android.location.Location
import com.fghbuild.sidekick.data.RoutePoint
import com.fghbuild.sidekick.data.RunData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class RunManager {
    private val _runData = MutableStateFlow(RunData())
    val runData: StateFlow<RunData> = _runData.asStateFlow()

    private var startTimeMillis: Long = 0
    private var lastLocation: Location? = null
    private var pausedTimeMillis: Long = 0

    fun startRun() {
        startTimeMillis = System.currentTimeMillis()
        _runData.value = RunData(isRunning = true)
    }

    fun pauseRun() {
        pausedTimeMillis = System.currentTimeMillis()
        _runData.value = _runData.value.copy(isRunning = false)
    }

    fun resumeRun() {
        if (!_runData.value.isRunning) {
            startTimeMillis += System.currentTimeMillis() - pausedTimeMillis
            _runData.value = _runData.value.copy(isRunning = true)
        }
    }

    fun updateLocation(location: Location) {
        val currentData = _runData.value
        var distanceMeters = currentData.distanceMeters

        lastLocation?.let {
            distanceMeters +=
                calculateDistance(
                    it.latitude,
                    it.longitude,
                    location.latitude,
                    location.longitude,
                )
        }

        val durationMillis = System.currentTimeMillis() - startTimeMillis
        val paceMinPerKm =
            if (distanceMeters > 0) {
                (durationMillis / 1000.0 / 60.0) / (distanceMeters / 1000.0)
            } else {
                0.0
            }

        lastLocation = location

        // Track pace history (keep all entries)
        val paceHistory = currentData.paceHistory + paceMinPerKm

        _runData.value =
            currentData.copy(
                distanceMeters = distanceMeters,
                paceMinPerKm = paceMinPerKm,
                durationMillis = durationMillis,
                paceHistory = paceHistory,
            )
    }

    fun updateRoutePoints(points: List<RoutePoint>) {
        _runData.value = _runData.value.copy(routePoints = points)
    }

    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a =
            sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c * 1000 // Convert to meters
    }
}
