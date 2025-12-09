// Run state and metrics management with Kalman filtering, distance/pace calculation, and auto-pause detection.
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
import com.fghbuild.sidekick.util.GpsFilteringUtils
import com.fghbuild.sidekick.util.KalmanNoiseDerivationUtils
import com.fghbuild.sidekick.util.PaceUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RunManager(
    private val gpsMeasurementDao: GpsMeasurementDao,
    private val gpsCalibrationDao: GpsCalibrationDao,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
) {
    private val _runData = MutableStateFlow(RunData())
    val runData: StateFlow<RunData> = _runData.asStateFlow()

    val defaultHeartRateData: StateFlow<HeartRateData> = MutableStateFlow(HeartRateData()).asStateFlow()

    private val _isAutoPaused = MutableStateFlow(false)
    val isAutoPaused: StateFlow<Boolean> = _isAutoPaused.asStateFlow()

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
    private var tickJob: Job? = null
    private var lastMovementTime: Long = 0
    private var isAutoPausedInternal = false
    private val noMovementThresholdMs = 5000L
    private val minMovementDistanceMeters = 1.0

    fun startRun() {
        startTimeMillis = System.currentTimeMillis()
        lastLocationTimeMillis = 0L
        lastLocation = null
        pausedTimeMillis = 0L
        runExplicitlyStarted = true
        kalmanFilterState = null
        kalmanFilterState = null
        previousRawRoutePoint = null
        lastMovementTime = System.currentTimeMillis()
        isAutoPausedInternal = false
        _isAutoPaused.value = false
        _runData.value = RunData(isRunning = true)
        startTickTimer()
    }

    fun pauseRun() {
        pausedTimeMillis = System.currentTimeMillis()
        _runData.value = _runData.value.copy(isRunning = false, isPaused = true)
        stopTickTimer()
        isAutoPausedInternal = false
        _isAutoPaused.value = false
    }

    fun resumeRun() {
        if (!_runData.value.isRunning) {
            startTimeMillis += System.currentTimeMillis() - pausedTimeMillis
            _runData.value = _runData.value.copy(isRunning = true, isPaused = false)
            startTickTimer()
            isAutoPausedInternal = false
            _isAutoPaused.value = false
        }
    }

    private fun autoPauseRun() {
        if (!isAutoPausedInternal && _runData.value.isRunning) {
            pausedTimeMillis = System.currentTimeMillis()
            _runData.value = _runData.value.copy(isRunning = false, isPaused = true)
            stopTickTimer()
            isAutoPausedInternal = true
            _isAutoPaused.value = true
        }
    }

    private fun autoResumeRun() {
        if (isAutoPausedInternal) {
            startTimeMillis += System.currentTimeMillis() - pausedTimeMillis
            _runData.value = _runData.value.copy(isRunning = true, isPaused = false)
            startTickTimer()
            isAutoPausedInternal = false
            _isAutoPaused.value = false
        }
    }

    fun stopRun() {
        _runData.value = _runData.value.copy(isRunning = false, isPaused = false)
        stopTickTimer()
    }

    fun updateLocation(location: Location) {
        val currentData = _runData.value

        // Allow processing if running or auto-paused (to continue GPS recording)
        if (!currentData.isRunning && !isAutoPausedInternal) {
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

        // 3. Check for movement to manage auto-pause/resume
        checkAndManageMovement(newRawRoutePoint)

        // Get or create calibration
        val calibration = currentCalibration ?: createDefaultCalibration(currentActivity)

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

        // Track pace history (only record meaningful pace values and skip during auto-pause)
        val paceHistory =
            updatePaceHistory(
                currentData.paceHistory,
                paceMinPerKm,
                distanceMeters,
                newRawRoutePoint.timestamp,
                !isAutoPausedInternal,
            )

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

    private fun checkAndManageMovement(newRoutePoint: RoutePoint) {
        val hasMovement = hasSignificantMovement(newRoutePoint)
        val currentTime = if (newRoutePoint.timestamp > 0) newRoutePoint.timestamp else System.currentTimeMillis()

        if (hasMovement) {
            lastMovementTime = currentTime
            // Auto-resume if currently auto-paused
            if (isAutoPausedInternal) {
                autoResumeRun()
            }
        } else {
            // Check if no movement for threshold duration
            val timeSinceLastMovement = currentTime - lastMovementTime
            if (timeSinceLastMovement >= noMovementThresholdMs && _runData.value.isRunning && !isAutoPausedInternal) {
                autoPauseRun()
            }
        }
    }

    private fun hasSignificantMovement(newRoutePoint: RoutePoint): Boolean {
        val lastFiltered = _runData.value.filteredRoutePoints.lastOrNull() ?: return true

        val distance =
            GeoUtils.calculateDistanceMeters(
                lastFiltered.latitude,
                lastFiltered.longitude,
                newRoutePoint.latitude,
                newRoutePoint.longitude,
            )

        return distance >= minMovementDistanceMeters
    }

    private fun createDefaultCalibration(
        activity: String,
        kalmanProcessNoise: Double = 0.02,
    ): GpsCalibrationEntity {
        return GpsCalibrationEntity(
            activity = activity,
            avgAccuracyMeters = 10.0,
            p95AccuracyMeters = 20.0,
            avgBearingAccuracyDegrees = 10.0,
            samplesCollected = 0,
            kalmanProcessNoise = kalmanProcessNoise,
            kalmanMeasurementNoise = 40.0,
            lastUpdated = System.currentTimeMillis(),
        )
    }

    private fun updatePaceHistory(
        currentHistory: List<PaceWithTime>,
        pace: Double,
        distance: Double,
        timestamp: Long,
        shouldRecord: Boolean = true,
    ): List<PaceWithTime> {
        return if (distance > 0 && pace > 0.0 && shouldRecord) {
            currentHistory + PaceWithTime(pace = pace, timestamp = timestamp)
        } else {
            currentHistory
        }
    }

    fun updateRoutePoints(points: List<RoutePoint>) {
        val currentData = _runData.value

        // Only update if we have new points
        if (points == currentData.routePoints) {
            return
        }

        // Apply full filtering for display purposes, including stationary removal
        val calibration =
            currentCalibration ?: createDefaultCalibration(currentActivity, kalmanProcessNoise = 0.001)

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
        val locationTime = fullyFilteredPoints.lastOrNull()?.timestamp ?: System.currentTimeMillis()
        val paceHistory =
            updatePaceHistory(
                currentData.paceHistory,
                paceMinPerKm,
                distanceMeters,
                locationTime,
            )

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
        stopTickTimer()
    }

    private fun startTickTimer() {
        stopTickTimer()
        tickJob =
            coroutineScope.launch {
                while (true) {
                    delay(1000)
                    updateTick()
                }
            }
    }

    private fun stopTickTimer() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun updateTick() {
        val currentData = _runData.value
        if (!currentData.isRunning) {
            return
        }

        val durationMillis = System.currentTimeMillis() - startTimeMillis

        _runData.value = currentData.copy(durationMillis = durationMillis)
    }

    private suspend fun updateCalibration(measurements: List<com.fghbuild.sidekick.database.GpsMeasurementEntity>) {
        val (avgAccuracy, p95Accuracy, avgBearingAccuracy) =
            KalmanNoiseDerivationUtils.calculateMeasurementStats(measurements)

        val kalmanNoise =
            KalmanNoiseDerivationUtils.deriveKalmanMeasurementNoise(
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
                        KalmanNoiseDerivationUtils.weightedAverage(
                            existing.avgAccuracyMeters,
                            existing.samplesCollected,
                            avgAccuracy,
                            measurements.size,
                        ),
                    p95AccuracyMeters =
                        KalmanNoiseDerivationUtils.weightedAverage(
                            existing.p95AccuracyMeters,
                            existing.samplesCollected,
                            p95Accuracy,
                            measurements.size,
                        ),
                    avgBearingAccuracyDegrees =
                        KalmanNoiseDerivationUtils.weightedAverage(
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
