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
                val locationTimestamp = if (locationTime > 0) locationTime else System.currentTimeMillis()
                currentData.paceHistory + PaceWithTime(pace = paceMinPerKm, timestamp = locationTimestamp)
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
