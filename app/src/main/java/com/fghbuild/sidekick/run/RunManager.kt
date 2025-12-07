package com.fghbuild.sidekick.run

import android.location.Location
import com.fghbuild.sidekick.data.HeartRateData
import com.fghbuild.sidekick.data.RoutePoint
import com.fghbuild.sidekick.data.RunData
import com.fghbuild.sidekick.util.GeoUtils
import com.fghbuild.sidekick.util.PaceUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RunManager {
    private val _runData = MutableStateFlow(RunData())
    val runData: StateFlow<RunData> = _runData.asStateFlow()

    val defaultHeartRateData: StateFlow<HeartRateData> = MutableStateFlow(HeartRateData()).asStateFlow()

    private var startTimeMillis: Long = 0
    private var lastLocation: Location? = null
    private var pausedTimeMillis: Long = 0
    private var lastLocationTimeMillis: Long = 0
    private var runExplicitlyStarted = false

    fun startRun() {
        startTimeMillis = System.currentTimeMillis()
        lastLocationTimeMillis = 0L
        lastLocation = null
        pausedTimeMillis = 0L
        runExplicitlyStarted = true
        _runData.value = RunData(isRunning = true)
    }

    fun pauseRun() {
        pausedTimeMillis = System.currentTimeMillis()
        _runData.value = _runData.value.copy(isRunning = false, isPaused = true)
    }

    fun resumeRun() {
        if (!_runData.value.isRunning) {
            startTimeMillis += System.currentTimeMillis() - pausedTimeMillis
            _runData.value = _runData.value.copy(isRunning = true, isPaused = false)
        }
    }

    fun stopRun() {
        _runData.value = _runData.value.copy(isRunning = false, isPaused = false)
    }

    fun updateLocation(location: Location) {
        val currentData = _runData.value
        if (!currentData.isRunning) {
            return
        }
        var distanceMeters = currentData.distanceMeters

        lastLocation?.let {
            distanceMeters +=
                GeoUtils.calculateDistanceMeters(
                    it.latitude,
                    it.longitude,
                    location.latitude,
                    location.longitude,
                )
        }

        // Use location timestamp if available (for test data with realistic timestamps)
        val locationTime = location.time

        // If run wasn't explicitly started and this is the first location, use its timestamp as start
        if (!runExplicitlyStarted && lastLocationTimeMillis == 0L && locationTime > 0) {
            startTimeMillis = locationTime
        }

        // Calculate duration using location timestamps when available
        val durationMillis =
            if (locationTime > 0) {
                // Use location timestamp but ensure it's not negative (handle race conditions)
                maxOf(0L, locationTime - startTimeMillis)
            } else {
                System.currentTimeMillis() - startTimeMillis
            }

        val paceMinPerKm = PaceUtils.calculatePaceMinPerKm(durationMillis, distanceMeters)

        lastLocation = location
        lastLocationTimeMillis = locationTime

        // Track pace history (only record meaningful pace values)
        val paceHistory =
            if (distanceMeters > 0 && paceMinPerKm > 0.0) {
                currentData.paceHistory + paceMinPerKm
            } else {
                currentData.paceHistory
            }

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
}
