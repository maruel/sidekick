package com.fghbuild.sidekick.util

import com.fghbuild.sidekick.data.RoutePoint
import com.fghbuild.sidekick.database.GpsCalibrationEntity

/**
 * Kalman filter implementation for GPS point filtering.
 * Removes noisy GPS measurements while preserving actual route information.
 */
object GpsFilteringUtils {
    /**
     * Represents the state of the Kalman filter at a given point.
     * This state is maintained across updates for incremental filtering.
     */
    data class KalmanState(
        val latitude: Double,
        val longitude: Double,
        val latitudeVariance: Double,
        val longitudeVariance: Double,
    )

    /**
     * Applies a Kalman filter to GPS route points using calibration parameters.
     *
     * @param points Input GPS route points (unfiltered)
     * @param calibration GPS calibration data with Kalman noise parameters
     * @return Filtered route points with smoothed coordinates
     */
    fun filterRoutePoints(
        points: List<RoutePoint>,
        calibration: GpsCalibrationEntity,
    ): List<RoutePoint> {
        if (points.isEmpty()) return emptyList()
        if (points.size == 1) return points

        val filtered = mutableListOf<RoutePoint>()
        var state =
            KalmanState(
                latitude = points[0].latitude,
                longitude = points[0].longitude,
                latitudeVariance = calibration.p95AccuracyMeters * calibration.p95AccuracyMeters,
                longitudeVariance = calibration.p95AccuracyMeters * calibration.p95AccuracyMeters,
            )

        // Add first point
        filtered.add(points[0].copy(latitude = state.latitude, longitude = state.longitude))

        // Apply Kalman filter to remaining points
        for (i in 1 until points.size) {
            val measurement = points[i]
            val dt = (measurement.timestamp - points[i - 1].timestamp) / 1000.0 // seconds

            // Prediction step: estimate process noise based on time delta
            val processNoise = calibration.kalmanProcessNoise * maxOf(1.0, dt)
            val predictedLatitudeVariance = state.latitudeVariance + processNoise
            val predictedLongitudeVariance = state.longitudeVariance + processNoise

            // Measurement noise based on calibration
            val measurementNoise = calibration.kalmanMeasurementNoise

            // Update step: Kalman gain
            val latitudeGain =
                predictedLatitudeVariance / (predictedLatitudeVariance + measurementNoise)
            val longitudeGain =
                predictedLongitudeVariance / (predictedLongitudeVariance + measurementNoise)

            // Updated state
            state =
                KalmanState(
                    latitude = state.latitude + latitudeGain * (measurement.latitude - state.latitude),
                    longitude =
                        state.longitude + longitudeGain * (measurement.longitude - state.longitude),
                    latitudeVariance = (1 - latitudeGain) * predictedLatitudeVariance,
                    longitudeVariance = (1 - longitudeGain) * predictedLongitudeVariance,
                )

            filtered.add(
                measurement.copy(
                    latitude = state.latitude,
                    longitude = state.longitude,
                ),
            )
        }

        return filtered
    }

    /**
     * Filters route points by removing stationary points (where device hasn't moved).
     * Useful for removing GPS jitter when runner is standing still.
     *
     * @param points Input route points
     * @param minDistanceMeters Minimum distance to consider as movement (default: 2 meters)
     * @return Filtered points with stationary segments removed
     */
    fun removeStationaryPoints(
        points: List<RoutePoint>,
        minDistanceMeters: Double = 0.5,
    ): List<RoutePoint> {
        if (points.size <= 1) return points

        val filtered = mutableListOf(points[0])

        for (i in 1 until points.size) {
            val lastPoint = filtered.last()
            val currentPoint = points[i]

            val distance =
                GeoUtils.calculateDistanceMeters(
                    lastPoint.latitude,
                    lastPoint.longitude,
                    currentPoint.latitude,
                    currentPoint.longitude,
                )

            // Only add point if it represents meaningful movement
            if (distance >= minDistanceMeters) {
                filtered.add(currentPoint)
            }
        }

        return filtered
    }

    /**
     * Combines Kalman filtering and stationary point removal.
     *
     * @param points Input GPS route points
     * @param calibration GPS calibration data
     * @param minDistanceMeters Minimum distance to consider as movement
     * @return Fully filtered route points
     */
    fun filterRoutePointsFully(
        points: List<RoutePoint>,
        calibration: GpsCalibrationEntity,
        minDistanceMeters: Double = 2.0,
    ): List<RoutePoint> {
        // First apply Kalman filter to reduce noise
        val kalmanFiltered = filterRoutePoints(points, calibration)

        // Then remove stationary points
        return removeStationaryPoints(kalmanFiltered, minDistanceMeters)
    }

    /**
     * Filters a single new point incrementally using existing Kalman filter state.
     * Maintains the filter state across multiple calls for efficiency.
     *
     * @param newPoint The new GPS measurement to filter
     * @param previousRawPoint The previous raw (unfiltered) point for time delta
     * @param currentState The current Kalman filter state
     * @param calibration GPS calibration data with Kalman noise parameters
     * @return Pair of (filtered point, updated Kalman state)
     */
    fun filterPointIncremental(
        newPoint: RoutePoint,
        previousRawPoint: RoutePoint,
        currentState: KalmanState,
        calibration: GpsCalibrationEntity,
    ): Pair<RoutePoint, KalmanState> {
        val dt = (newPoint.timestamp - previousRawPoint.timestamp) / 1000.0 // seconds

        // Prediction step: estimate process noise based on time delta
        val processNoise = calibration.kalmanProcessNoise * maxOf(1.0, dt)
        val predictedLatitudeVariance = currentState.latitudeVariance + processNoise
        val predictedLongitudeVariance = currentState.longitudeVariance + processNoise

        // Measurement noise based on calibration
        val measurementNoise = calibration.kalmanMeasurementNoise

        // Update step: Kalman gain
        val latitudeGain =
            predictedLatitudeVariance / (predictedLatitudeVariance + measurementNoise)
        val longitudeGain =
            predictedLongitudeVariance / (predictedLongitudeVariance + measurementNoise)

        // Updated state
        val newState =
            KalmanState(
                latitude = currentState.latitude + latitudeGain * (newPoint.latitude - currentState.latitude),
                longitude =
                    currentState.longitude + longitudeGain * (newPoint.longitude - currentState.longitude),
                latitudeVariance = (1 - latitudeGain) * predictedLatitudeVariance,
                longitudeVariance = (1 - longitudeGain) * predictedLongitudeVariance,
            )

        val filteredPoint =
            newPoint.copy(
                latitude = newState.latitude,
                longitude = newState.longitude,
            )

        return Pair(filteredPoint, newState)
    }

    /**
     * Initializes Kalman filter state for the first point.
     *
     * @param firstPoint The first GPS measurement
     * @param calibration GPS calibration data
     * @return Initial Kalman filter state
     */
    fun initializeKalmanState(
        firstPoint: RoutePoint,
        calibration: GpsCalibrationEntity,
    ): KalmanState =
        KalmanState(
            latitude = firstPoint.latitude,
            longitude = firstPoint.longitude,
            latitudeVariance = calibration.p95AccuracyMeters * calibration.p95AccuracyMeters,
            longitudeVariance = calibration.p95AccuracyMeters * calibration.p95AccuracyMeters,
        )

    /**
     * Validates if a point should be included based on accuracy threshold.
     * Rejects points with poor GPS accuracy.
     *
     * @param latitude Point latitude
     * @param longitude Point longitude
     * @param accuracy GPS accuracy estimate in meters
     * @param maxAccuracyMeters Maximum acceptable accuracy (default: p95 accuracy threshold)
     * @return True if point should be included
     */
    fun isPointValid(
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        maxAccuracyMeters: Double = 25.0,
    ): Boolean {
        // Check for valid coordinates
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            return false
        }

        // Check accuracy threshold
        return accuracy > 0 && accuracy <= maxAccuracyMeters
    }
}
