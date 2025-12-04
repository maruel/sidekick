package com.fghbuild.sidekick.fixtures

import com.fghbuild.sidekick.data.HeartRateData
import com.fghbuild.sidekick.data.RoutePoint
import com.fghbuild.sidekick.data.RunData
import com.fghbuild.sidekick.database.RoutePointEntity
import com.fghbuild.sidekick.database.RunEntity
import com.fghbuild.sidekick.util.GeoUtils
import kotlin.math.cos
import kotlin.math.sin

/**
 * Factory for creating realistic test data.
 * Generates deterministic GPS routes and heart rate data for testing.
 */
object TestDataFactory {
    /**
     * Creates a test route with known distance.
     * Simulates a runner moving in a rectangle at a constant pace.
     *
     * @param distanceKm Target distance in kilometers
     * @param startLatitude Starting latitude (default: San Francisco)
     * @param startLongitude Starting longitude
     * @return List of route points representing the path
     */
    fun createTestRoute(
        distanceKm: Double,
        startLatitude: Double = 37.7749,
        startLongitude: Double = -122.4194,
    ): List<RoutePoint> {
        val points = mutableListOf<RoutePoint>()
        var currentLat = startLatitude
        var currentLon = startLongitude
        var distanceCovered = 0.0
        var timestamp = System.currentTimeMillis()
        val intervalMs = 1000L // 1 second between points

        // Move in a spiral pattern to cover distance
        var segmentDistance = distanceKm / 4.0 // 4 segments in a rectangle
        var segmentIndex = 0
        val directions = listOf(0.0, 90.0, 180.0, 270.0) // North, East, South, West

        points.add(RoutePoint(currentLat, currentLon, timestamp))

        while (distanceCovered < distanceKm) {
            val direction = directions[segmentIndex % 4]
            val radians = Math.toRadians(direction)

            // Move 0.001 degrees per point (roughly 111 meters on Earth)
            val latDelta = 0.001 * cos(radians)
            val lonDelta = 0.001 * sin(radians)

            currentLat += latDelta
            currentLon += lonDelta
            timestamp += intervalMs

            val point = RoutePoint(currentLat, currentLon, timestamp)
            points.add(point)

            // Calculate actual distance covered
            if (points.size > 1) {
                val lastPoint = points[points.size - 2]
                val segmentDistance =
                    GeoUtils.distanceBetweenPoints(
                        lastPoint.latitude,
                        lastPoint.longitude,
                        point.latitude,
                        point.longitude,
                    )
                distanceCovered += segmentDistance / 1000.0 // Convert to km
            }

            // Move to next segment if current segment is complete
            if (distanceCovered >= (segmentIndex + 1) * segmentDistance / 4.0) {
                segmentIndex++
            }
        }

        return points
    }

    /**
     * Creates heart rate data simulating a run.
     *
     * @param count Number of measurements
     * @param minBpm Minimum heart rate
     * @param maxBpm Maximum heart rate
     * @return HeartRateData with realistic values
     */
    fun createHeartRateData(
        count: Int = 100,
        minBpm: Int = 120,
        maxBpm: Int = 180,
    ): HeartRateData {
        require(count > 0) { "Count must be positive" }
        require(maxBpm > minBpm) { "Max BPM must be greater than min" }

        val measurements = mutableListOf<Int>()

        // Simulate HR curve: warmup -> steady -> cooldown
        val warmupPhase = count / 4
        val steadyPhase = count / 2
        val cooldownPhase = count - warmupPhase - steadyPhase

        // Warmup: linear increase
        for (i in 0 until warmupPhase) {
            val ratio = i.toDouble() / warmupPhase
            val bpm = (minBpm + (maxBpm - minBpm) * ratio).toInt()
            measurements.add(bpm)
        }

        // Steady: fluctuate around max
        for (i in 0 until steadyPhase) {
            val variation = ((i % 20) - 10) * 2 // ±20 variation
            val bpm = (maxBpm - 20 + variation).coerceIn(minBpm, maxBpm)
            measurements.add(bpm)
        }

        // Cooldown: linear decrease
        for (i in 0 until cooldownPhase) {
            val ratio = i.toDouble() / cooldownPhase
            val bpm = (maxBpm - (maxBpm - minBpm) * ratio).toInt()
            measurements.add(bpm)
        }

        val averageBpm = measurements.average().toInt()

        return HeartRateData(
            currentBpm = measurements.lastOrNull() ?: minBpm,
            averageBpm = averageBpm,
            measurements = measurements,
        )
    }

    /**
     * Creates a complete test run.
     */
    fun createTestRunData(
        distanceKm: Double = 5.0,
        durationMinutes: Int = 45,
    ): RunData {
        val route = createTestRoute(distanceKm)
        val paceHistory =
            List(durationMinutes) { 9.0 + (Math.random() * 2.0 - 1.0) } // 8-10 min/km

        return RunData(
            distanceMeters = distanceKm * 1000,
            paceMinPerKm = paceHistory.average(),
            durationMillis = (durationMinutes * 60 * 1000).toLong(),
            routePoints = route,
            paceHistory = paceHistory,
            isRunning = false,
        )
    }

    /**
     * Creates a test run entity for database operations.
     */
    fun createTestRunEntity(
        id: Long = 0L,
        distanceMeters: Double = 5000.0,
        durationMinutes: Int = 45,
        averagePaceMinPerKm: Double = 9.0,
        maxHeartRate: Int = 180,
        minHeartRate: Int = 100,
        averageHeartRate: Int = 150,
    ): RunEntity {
        val now = System.currentTimeMillis()
        val startTime = now - (durationMinutes * 60 * 1000).toLong()
        val endTime = now

        return RunEntity(
            id = id,
            startTime = startTime,
            endTime = endTime,
            distanceMeters = distanceMeters,
            durationMillis = (durationMinutes * 60 * 1000).toLong(),
            averagePaceMinPerKm = averagePaceMinPerKm,
            maxHeartRate = maxHeartRate,
            minHeartRate = minHeartRate,
            averageHeartRate = averageHeartRate,
        )
    }

    /**
     * Creates test route point entities for a run.
     */
    fun createTestRoutePointEntities(
        runId: Long,
        distanceKm: Double = 5.0,
    ): List<RoutePointEntity> {
        val routePoints = createTestRoute(distanceKm)
        return routePoints.map { point ->
            RoutePointEntity(
                runId = runId,
                latitude = point.latitude,
                longitude = point.longitude,
                timestamp = point.timestamp,
            )
        }
    }
}
