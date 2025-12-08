package com.fghbuild.sidekick.run

import android.location.Location
import com.fghbuild.sidekick.data.HeartRateData
import com.fghbuild.sidekick.data.HeartRateWithTime
import com.fghbuild.sidekick.data.PaceWithTime
import com.fghbuild.sidekick.data.RoutePoint
import com.fghbuild.sidekick.data.RunData
import com.fghbuild.sidekick.database.GpsCalibrationDao
import com.fghbuild.sidekick.database.GpsCalibrationEntity
import com.fghbuild.sidekick.database.GpsMeasurementDao
import com.fghbuild.sidekick.util.GeoUtils
import com.fghbuild.sidekick.util.GpsCalibrationUtils
import com.fghbuild.sidekick.util.GpsFilteringUtils
import com.fghbuild.sidekick.util.PaceUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RunManager(
    private val gpsMeasurementDao: GpsMeasurementDao,
    private val gpsCalibrationDao: GpsCalibrationDao,
) {
    private val _runData = MutableStateFlow(RunData())
    val runData: StateFlow<RunData> = _runData.asStateFlow()

    val defaultHeartRateData: StateFlow<HeartRateData> = MutableStateFlow(HeartRateData()).asStateFlow()

    private var startTimeMillis: Long = 0
    private var lastLocation: Location? = null
    private var pausedTimeMillis: Long = 0
    private var lastLocationTimeMillis: Long = 0
    private var runExplicitlyStarted = false
    private var currentRunId: Long? = null
    private var currentActivity: String = "running"
    private var currentCalibration: GpsCalibrationEntity? = null
    private var kalmanFilterState: GpsFilteringUtils.KalmanState? = null
    private var previousRawRoutePoint: RoutePoint? = null

    fun startRun() {
        startTimeMillis = System.currentTimeMillis()
        lastLocationTimeMillis = 0L
        lastLocation = null
        pausedTimeMillis = 0L
        runExplicitlyStarted = true
        kalmanFilterState = null
        kalmanFilterState = null
        previousRawRoutePoint = null
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

        // 1. Convert Android Location to RoutePoint
        val newRawRoutePoint =
            RoutePoint(
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = location.time,
                accuracy = location.accuracy,
                bearing = location.bearing,
                speed = location.speed,
            )

        // 2. Validate point before processing
        if (!GpsFilteringUtils.isPointValid(
                newRawRoutePoint.latitude,
                newRawRoutePoint.longitude,
                newRawRoutePoint.accuracy,
            )
        ) {
            // Optionally, we could log this or update UI with a "GPS signal weak" message
            return
        }

        // Get or create calibration
        val calibration =
            currentCalibration ?: GpsCalibrationEntity(
                activity = currentActivity,
                avgAccuracyMeters = 10.0,
                p95AccuracyMeters = 20.0,
                avgBearingAccuracyDegrees = 10.0,
                samplesCollected = 0,
                kalmanProcessNoise = 0.001,
                kalmanMeasurementNoise = 100.0,
                lastUpdated = System.currentTimeMillis(),
            )

        var newFilteredPoint: RoutePoint = newRawRoutePoint
        if (kalmanFilterState == null) {
            // First valid point, initialize Kalman filter
            kalmanFilterState = GpsFilteringUtils.initializeKalmanState(newRawRoutePoint, calibration)
            newFilteredPoint =
                newRawRoutePoint.copy(
                    latitude = kalmanFilterState!!.latitude,
                    longitude = kalmanFilterState!!.longitude,
                )
        } else {
            // Subsequent points, incrementally filter
            if (previousRawRoutePoint != null) {
                val (filtered, newState) =
                    GpsFilteringUtils.filterPointIncremental(
                        newRawRoutePoint,
                        previousRawRoutePoint!!,
                        kalmanFilterState!!,
                        calibration,
                    )
                newFilteredPoint = filtered
                kalmanFilterState = newState
            }
        }
        previousRawRoutePoint = newRawRoutePoint

        // If run wasn't explicitly started and this is the first location, use its timestamp as start
        if (!runExplicitlyStarted && lastLocationTimeMillis == 0L && newRawRoutePoint.timestamp > 0) {
            startTimeMillis = newRawRoutePoint.timestamp
        }

        val durationMillis =
            if (newRawRoutePoint.timestamp > 0) {
                // Use location timestamp but ensure it's not negative (handle race conditions)
                maxOf(0L, newRawRoutePoint.timestamp - startTimeMillis)
            } else {
                System.currentTimeMillis() - startTimeMillis
            }

        // Calculate distance using filtered points
        var distanceMeters = currentData.distanceMeters
        currentData.filteredRoutePoints.lastOrNull()?.let { lastFilteredPoint ->
            distanceMeters +=
                GeoUtils.calculateDistanceMeters(
                    lastFilteredPoint.latitude,
                    lastFilteredPoint.longitude,
                    newFilteredPoint.latitude,
                    newFilteredPoint.longitude,
                )
        }

        val paceMinPerKm = PaceUtils.calculatePaceMinPerKm(durationMillis, distanceMeters)

        lastLocation = location // Keep lastLocation as raw Android Location for consistency
        lastLocationTimeMillis = newRawRoutePoint.timestamp

        // Track pace history (only record meaningful pace values)
        val paceHistory =
            if (distanceMeters > 0 && paceMinPerKm > 0.0) {
                currentData.paceHistory + PaceWithTime(pace = paceMinPerKm, timestamp = newRawRoutePoint.timestamp)
            } else {
                currentData.paceHistory
            }

        _runData.value =
            currentData.copy(
                distanceMeters = distanceMeters,
                paceMinPerKm = paceMinPerKm,
                durationMillis = durationMillis,
                // Add raw and filtered points
                routePoints = currentData.routePoints + newRawRoutePoint,
                filteredRoutePoints = currentData.filteredRoutePoints + newFilteredPoint,
                paceHistory = paceHistory,
            )
    }

    fun updateRoutePoints(points: List<RoutePoint>) {
        val currentData = _runData.value

        // Only update if we have new points
        if (points == currentData.routePoints) {
            return
        }

        // Apply full filtering for display purposes, including stationary removal
        val calibration =
            currentCalibration ?: GpsCalibrationEntity(
                activity = currentActivity,
                avgAccuracyMeters = 10.0,
                p95AccuracyMeters = 20.0,
                avgBearingAccuracyDegrees = 10.0,
                samplesCollected = 0,
                kalmanProcessNoise = 0.001,
                kalmanMeasurementNoise = 100.0,
                lastUpdated = System.currentTimeMillis(),
            )

        val fullyFilteredPoints = GpsFilteringUtils.filterRoutePointsFully(points, calibration)

        // Recalculate distance using fully filtered points for display
        var distanceMeters = 0.0
        for (i in 1 until fullyFilteredPoints.size) {
            distanceMeters +=
                GeoUtils.calculateDistanceMeters(
                    fullyFilteredPoints[i - 1].latitude,
                    fullyFilteredPoints[i - 1].longitude,
                    fullyFilteredPoints[i].latitude,
                    fullyFilteredPoints[i].longitude,
                )
        }

        // Recalculate pace using fully filtered points
        val paceMinPerKm = PaceUtils.calculatePaceMinPerKm(currentData.durationMillis, distanceMeters)

        // Update pace history with recalculated pace
        val paceHistory =
            if (distanceMeters > 0 && paceMinPerKm > 0.0) {
                val locationTime = fullyFilteredPoints.lastOrNull()?.timestamp ?: System.currentTimeMillis()
                currentData.paceHistory + PaceWithTime(pace = paceMinPerKm, timestamp = locationTime)
            } else {
                currentData.paceHistory
            }

        _runData.value =
            currentData.copy(
                routePoints = points,
                filteredRoutePoints = fullyFilteredPoints,
                distanceMeters = distanceMeters,
                paceMinPerKm = paceMinPerKm,
                paceHistory = paceHistory,
            )
    }

    fun updateHeartRate(bpm: Int) {
        val currentData = _runData.value
        if (!currentData.isRunning || bpm <= 0) {
            return
        }

        val heartRateHistory =
            currentData.heartRateHistory +
                HeartRateWithTime(
                    bpm = bpm,
                    timestamp = System.currentTimeMillis(),
                )

        _runData.value =
            currentData.copy(
                heartRateHistory = heartRateHistory,
            )
    }

    suspend fun initializeRunSession(
        runId: Long,
        activity: String,
    ) {
        currentRunId = runId
        currentActivity = activity
        // Load calibration data for this activity to use in filtering
        currentCalibration = gpsCalibrationDao.getCalibration(activity)
    }

    suspend fun finalizeRunSession() {
        currentRunId?.let { runId ->
            // Only update calibration if session was at least 30 seconds
            val durationMillis = _runData.value.durationMillis
            if (durationMillis >= 30000) {
                val measurements = gpsMeasurementDao.getRunMeasurements(runId)
                if (measurements.isNotEmpty()) {
                    updateCalibration(measurements)
                }
            }
        }
        currentRunId = null
    }

    private suspend fun updateCalibration(measurements: List<com.fghbuild.sidekick.database.GpsMeasurementEntity>) {
        val (avgAccuracy, p95Accuracy, avgBearingAccuracy) =
            GpsCalibrationUtils.calculateMeasurementStats(measurements)

        val kalmanNoise =
            GpsCalibrationUtils.deriveKalmanMeasurementNoise(
                measurements.map { it.accuracy },
            )

        // Get existing calibration for this activity
        val existing = gpsCalibrationDao.getCalibration(currentActivity)

        val newCalibration =
            if (existing != null) {
                // Merge with weighted average
                GpsCalibrationEntity(
                    activity = currentActivity,
                    avgAccuracyMeters =
                        GpsCalibrationUtils.weightedAverage(
                            existing.avgAccuracyMeters,
                            existing.samplesCollected,
                            avgAccuracy,
                            measurements.size,
                        ),
                    p95AccuracyMeters =
                        GpsCalibrationUtils.weightedAverage(
                            existing.p95AccuracyMeters,
                            existing.samplesCollected,
                            p95Accuracy,
                            measurements.size,
                        ),
                    avgBearingAccuracyDegrees =
                        GpsCalibrationUtils.weightedAverage(
                            existing.avgBearingAccuracyDegrees,
                            existing.samplesCollected,
                            avgBearingAccuracy,
                            measurements.size,
                        ),
                    samplesCollected = existing.samplesCollected + measurements.size,
                    kalmanProcessNoise = existing.kalmanProcessNoise,
                    kalmanMeasurementNoise = kalmanNoise,
                    lastUpdated = System.currentTimeMillis(),
                )
            } else {
                // First calibration for this activity
                GpsCalibrationEntity(
                    activity = currentActivity,
                    avgAccuracyMeters = avgAccuracy,
                    p95AccuracyMeters = p95Accuracy,
                    avgBearingAccuracyDegrees = avgBearingAccuracy,
                    samplesCollected = measurements.size,
                    kalmanProcessNoise = 0.001,
                    kalmanMeasurementNoise = kalmanNoise,
                    lastUpdated = System.currentTimeMillis(),
                )
            }

        gpsCalibrationDao.upsert(newCalibration)
    }
}
