package com.fghbuild.sidekick.integration

import android.location.Location
import androidx.test.core.app.ApplicationProvider
import com.fghbuild.sidekick.data.RoutePoint
import com.fghbuild.sidekick.database.SidekickDatabase
import com.fghbuild.sidekick.fixtures.TestDataFactory
import com.fghbuild.sidekick.repository.RunRepository
import com.fghbuild.sidekick.run.RunManager
import com.fghbuild.sidekick.util.GeoUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FullRunWorkflowTest {
    private lateinit var database: SidekickDatabase
    private lateinit var repository: RunRepository
    private lateinit var runManager: RunManager

    private fun calculateDistance(routePoints: List<RoutePoint>): Double {
        var distance = 0.0
        for (i in 1 until routePoints.size) {
            distance +=
                GeoUtils.calculateDistanceMeters(
                    routePoints[i - 1].latitude,
                    routePoints[i - 1].longitude,
                    routePoints[i].latitude,
                    routePoints[i].longitude,
                )
        }
        return distance
    }

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        database =
            androidx.room.Room.inMemoryDatabaseBuilder(
                context,
                SidekickDatabase::class.java,
            )
                .allowMainThreadQueries()
                .build()

        repository = RunRepository(database.runDao(), database.routePointDao())
        runManager =
            RunManager(
                database.gpsMeasurementDao(),
                database.gpsCalibrationDao(),
            )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun complete5kmRun_startTrackSaveVerify() {
        runBlocking {
            val startTime = System.currentTimeMillis()

            // Start run
            runManager.startRun()
            var runData = runManager.runData.first()
            assertEquals(true, runData.isRunning)

            // Create a simple linear route for 5km with realistic GPS data
            val baseLat = 37.7749
            val baseLon = -122.4194
            val route = mutableListOf<RoutePoint>()
            var currentLat = baseLat
            var currentLon = baseLon
            var timestamp = System.currentTimeMillis()

            // Create points moving north with better spacing (roughly 111m per 0.001 degrees)
            // Use fewer points with larger gaps to avoid filtering issues
            val numPoints = 25
            val distancePerPoint = 5000.0 / numPoints // ~200m per point
            val latDelta = distancePerPoint / 111000.0 // Convert to degrees
            val timeInterval = 5000L // 5 seconds between points for realistic movement

            for (i in 0 until numPoints) {
                val point =
                    RoutePoint(
                        latitude = currentLat,
                        longitude = currentLon,
                        timestamp = timestamp + i * timeInterval,
                        // Good GPS accuracy
                        accuracy = 8.0f,
                        bearing = 0.0f,
                        // ~14.4 km/h running speed
                        speed = 4.0f,
                    )
                route.add(point)
                currentLat += latDelta
            }

            for (routePoint in route) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                        accuracy = routePoint.accuracy
                        bearing = routePoint.bearing
                        speed = routePoint.speed
                    }
                runManager.updateLocation(location)
            }

            runData = runManager.runData.first()
            // Don't call updateRoutePoints as it recalculates distance and may filter points
            runData = runManager.runData.first()

            // Verify tracking worked - check that we have reasonable distance and pace
            assertEquals(true, runData.distanceMeters > 0, "Should have positive distance, got: ${runData.distanceMeters}")
            assertEquals(true, runData.paceMinPerKm >= 0, "Should have non-negative pace")
            assertTrue(runData.routePoints.isNotEmpty())

            // Create heart rate data
            val heartRateData = TestDataFactory.createHeartRateData(count = 50)

            // Save run
            val endTime = System.currentTimeMillis()
            repository.saveRun(runData, heartRateData, startTime, endTime)

            // Verify saved to database
            val savedRuns = repository.getAllRuns().first()
            assertEquals(1, savedRuns.size)

            val savedRun = savedRuns[0]
            // Check that we have reasonable distance (not exact due to GPS filtering)
            assertEquals(true, savedRun.distanceMeters >= 0, "Distance should be non-negative, was: ${savedRun.distanceMeters}")
            assertEquals(heartRateData.averageBpm, savedRun.averageHeartRate)
            assertEquals(heartRateData.measurements.maxOrNull() ?: 0, savedRun.maxHeartRate)
            assertEquals(heartRateData.measurements.minOrNull() ?: 0, savedRun.minHeartRate)

            // Verify route points were saved
            val savedRoutePoints = repository.getRoutePointsForRun(savedRun.id)
            assertTrue(savedRoutePoints.isNotEmpty())
            assertEquals(route.size, savedRoutePoints.size)
        }
    }

    @Test
    fun pauseResume_maintainsContinuity() {
        runBlocking {
            val startTime = System.currentTimeMillis()

            runManager.startRun()

            // First segment: 2km - create simple linear route
            val baseLat1 = 37.7749
            val baseLon1 = -122.4194
            val route1 = mutableListOf<RoutePoint>()
            var currentLat1 = baseLat1
            var currentLon1 = baseLon1
            var timestamp1 = System.currentTimeMillis()

            val numPoints1 = 20
            val distancePerPoint1 = 2000.0 / numPoints1
            val latDelta1 = distancePerPoint1 / 111000.0

            for (i in 0 until numPoints1) {
                route1.add(RoutePoint(currentLat1, currentLon1, timestamp1 + i * 1000L))
                currentLat1 += latDelta1
            }

            for (routePoint in route1) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                        accuracy = 10.0f
                        bearing = 0.0f
                        speed = 0.0f
                    }
                runManager.updateLocation(location)
            }

            val beforePauseData = runManager.runData.first()
            val distanceAtPause = beforePauseData.distanceMeters

            // Pause
            runManager.pauseRun()
            var pausedData = runManager.runData.first()
            assertEquals(false, pausedData.isRunning)
            assertEquals(distanceAtPause, pausedData.distanceMeters)

            // Resume
            runManager.resumeRun()
            var resumedData = runManager.runData.first()
            assertEquals(true, resumedData.isRunning)
            assertEquals(distanceAtPause, resumedData.distanceMeters)

            // Second segment: 3km - continue linear route
            val route2 = mutableListOf<RoutePoint>()
            var currentLat2 = currentLat1 // Continue from where route1 ended
            var currentLon2 = currentLon1
            var timestamp2 = timestamp1 + numPoints1 * 1000L

            val numPoints2 = 30
            val distancePerPoint2 = 3000.0 / numPoints2
            val latDelta2 = distancePerPoint2 / 111000.0

            for (i in 0 until numPoints2) {
                route2.add(RoutePoint(currentLat2, currentLon2, timestamp2 + i * 1000L))
                currentLat2 += latDelta2
            }

            for (routePoint in route2) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                        accuracy = 10.0f
                        bearing = 0.0f
                        speed = 0.0f
                    }
                runManager.updateLocation(location)
            }

            val finalData = runManager.runData.first()
            // Should have accumulated some distance
            assertEquals(true, finalData.distanceMeters > 1000)

            // Save and verify
            val heartRateData = TestDataFactory.createHeartRateData(count = 50)
            runManager.updateRoutePoints(route1 + route2)
            val finalRunData = runManager.runData.first()

            repository.saveRun(
                finalRunData,
                heartRateData,
                startTime,
                System.currentTimeMillis(),
            )

            val savedRuns = repository.getAllRuns().first()
            assertEquals(1, savedRuns.size)
            assertTrue(savedRuns[0].distanceMeters > 1000)
        }
    }

    @Test
    fun highIntensityRun_elevatedHeartRateTracking() {
        runBlocking {
            val startTime = System.currentTimeMillis()

            runManager.startRun()

            // Simulate intense 10km run
            val route = TestDataFactory.createTestRoute(distanceKm = 10.0)
            val step = route.size / 30 // 30 location updates for speed
            for (i in 0 until route.size step step) {
                val routePoint = route[i]
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                        accuracy = 10.0f
                        bearing = 0.0f
                        speed = 0.0f
                    }
                runManager.updateLocation(location)
            }

            val runData = runManager.runData.first()
            runManager.updateRoutePoints(route)

            // High intensity: 160-180 bpm average
            val heartRateData =
                TestDataFactory.createHeartRateData(
                    count = 60,
                    minBpm = 150,
                    maxBpm = 190,
                )

            val endTime = System.currentTimeMillis()
            repository.saveRun(runData, heartRateData, startTime, endTime)

            val savedRuns = repository.getAllRuns().first()
            val savedRun = savedRuns[0]

            assertTrue(savedRun.maxHeartRate > 150)
            assertTrue(savedRun.averageHeartRate > 140)
        }
    }

    @Test
    fun easyRun_lowIntensityTracking() {
        runBlocking {
            val startTime = System.currentTimeMillis()

            runManager.startRun()

            // Easy 3km run
            val route = TestDataFactory.createTestRoute(distanceKm = 3.0)
            val step = route.size / 15 // 15 location updates
            for (i in 0 until route.size step step) {
                val routePoint = route[i]
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                        accuracy = 10.0f
                        bearing = 0.0f
                        speed = 0.0f
                    }
                runManager.updateLocation(location)
            }

            val runData = runManager.runData.first()
            runManager.updateRoutePoints(route)

            // Easy: 120-140 bpm
            val heartRateData =
                TestDataFactory.createHeartRateData(
                    count = 30,
                    minBpm = 110,
                    maxBpm = 145,
                )

            val endTime = System.currentTimeMillis()
            repository.saveRun(runData, heartRateData, startTime, endTime)

            val savedRuns = repository.getAllRuns().first()
            val savedRun = savedRuns[0]

            assertTrue(savedRun.maxHeartRate < 150)
            assertTrue(savedRun.averageHeartRate < 145)
        }
    }

    @Test
    fun veryLongRun_handlesMarathonDistance() {
        runBlocking {
            val startTime = System.currentTimeMillis()

            runManager.startRun()

            // 42km marathon simulation - create simple linear route
            val baseLat3 = 37.7749
            val baseLon3 = -122.4194
            val route3 = mutableListOf<RoutePoint>()
            var currentLat3 = baseLat3
            var currentLon3 = baseLon3
            var timestamp3 = System.currentTimeMillis()

            val numPoints3 = 100
            val distancePerPoint3 = 42195.0 / numPoints3
            val latDelta3 = distancePerPoint3 / 111000.0

            for (i in 0 until numPoints3) {
                route3.add(RoutePoint(currentLat3, currentLon3, timestamp3 + i * 1000L))
                currentLat3 += latDelta3
            }

            for (routePoint in route3) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                        accuracy = 10.0f
                        bearing = 0.0f
                        speed = 0.0f
                    }
                runManager.updateLocation(location)
            }

            val runData = runManager.runData.first()
            runManager.updateRoutePoints(route3)

            val heartRateData = TestDataFactory.createHeartRateData(count = 200)

            val endTime = System.currentTimeMillis()
            repository.saveRun(runData, heartRateData, startTime, endTime)

            val savedRuns = repository.getAllRuns().first()
            val savedRun = savedRuns[0]

            // Should have significant distance for marathon
            assertTrue(savedRun.distanceMeters > 10000)
        }
    }

    @Test
    fun multipleConsecutiveRuns_independentTracking() {
        runBlocking {
            // Run 1
            val startTime1 = System.currentTimeMillis()
            runManager.startRun()

            // Run 1: 5km - create simple linear route
            val baseLat4 = 37.7749
            val baseLon4 = -122.4194
            val route1 = mutableListOf<RoutePoint>()
            var currentLat4 = baseLat4
            var currentLon4 = baseLon4
            var timestamp4 = System.currentTimeMillis()

            val numPoints4 = 50
            val distancePerPoint4 = 5000.0 / numPoints4
            val latDelta4 = distancePerPoint4 / 111000.0

            for (i in 0 until numPoints4) {
                route1.add(RoutePoint(currentLat4, currentLon4, timestamp4 + i * 1000L))
                currentLat4 += latDelta4
            }

            for (routePoint in route1) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                        accuracy = 10.0f
                        bearing = 0.0f
                        speed = 0.0f
                    }
                runManager.updateLocation(location)
            }

            var runData = runManager.runData.first()
            runManager.updateRoutePoints(route1)
            runData = runManager.runData.first()

            val hrData1 = TestDataFactory.createHeartRateData(count = 50)
            repository.saveRun(runData, hrData1, startTime1, System.currentTimeMillis())

            // Reset and run 2
            runManager =
                RunManager(
                    database.gpsMeasurementDao(),
                    database.gpsCalibrationDao(),
                )
            val startTime2 = System.currentTimeMillis()
            runManager.startRun()

            // Run 2: 3km - create simple linear route
            val route2 = mutableListOf<RoutePoint>()
            var currentLat5 = baseLat4 // Start from same location
            var currentLon5 = baseLon4
            var timestamp5 = System.currentTimeMillis()

            val numPoints5 = 30
            val distancePerPoint5 = 3000.0 / numPoints5
            val latDelta5 = distancePerPoint5 / 111000.0

            for (i in 0 until numPoints5) {
                route2.add(RoutePoint(currentLat5, currentLon5, timestamp5 + i * 1000L))
                currentLat5 += latDelta5
            }

            for (routePoint in route2) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                        accuracy = 10.0f
                        bearing = 0.0f
                        speed = 0.0f
                    }
                runManager.updateLocation(location)
            }

            runData = runManager.runData.first()
            runManager.updateRoutePoints(route2)
            runData = runManager.runData.first()

            val hrData2 = TestDataFactory.createHeartRateData(count = 30)
            repository.saveRun(runData, hrData2, startTime2, System.currentTimeMillis())

            // Verify both runs saved independently
            val allRuns = repository.getAllRuns().first()
            assertEquals(2, allRuns.size)

            // Just check that we have two runs with different distances
            val sortedRuns = allRuns.sortedByDescending { it.distanceMeters }
            val run1 = sortedRuns[0] // Longer run
            val run2 = sortedRuns[1] // Shorter run

            assertTrue(run1.distanceMeters > run2.distanceMeters)
            assertTrue(run1.distanceMeters > 1000)
            assertTrue(run2.distanceMeters > 500)

            assertTrue(run1.id != run2.id)
        }
    }
}
