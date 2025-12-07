package com.fghbuild.sidekick.integration

import android.location.Location
import androidx.test.core.app.ApplicationProvider
import com.fghbuild.sidekick.database.SidekickDatabase
import com.fghbuild.sidekick.fixtures.TestDataFactory
import com.fghbuild.sidekick.repository.RunRepository
import com.fghbuild.sidekick.run.RunManager
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

            // Simulate 5km run with location updates
            val route = TestDataFactory.createTestRoute(distanceKm = 5.0)
            for (routePoint in route) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                    }
                runManager.updateLocation(location)
            }

            runData = runManager.runData.first()
            runManager.updateRoutePoints(route)
            runData = runManager.runData.first()

            // Verify tracking worked
            assertEquals(true, runData.distanceMeters > 4900)
            assertEquals(true, runData.distanceMeters < 5100)
            assertTrue(runData.paceMinPerKm > 0)
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
            assertEquals(true, savedRun.distanceMeters > 4900)
            assertEquals(true, savedRun.distanceMeters < 5100)
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

            // First segment: 2km
            val route1 = TestDataFactory.createTestRoute(distanceKm = 2.0)
            for (routePoint in route1) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
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

            // Second segment: 3km
            val route2 = TestDataFactory.createTestRoute(distanceKm = 3.0)
            for (routePoint in route2) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                    }
                runManager.updateLocation(location)
            }

            val finalData = runManager.runData.first()
            // Should be roughly 5km total
            assertEquals(true, finalData.distanceMeters > 4900)

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
            assertTrue(savedRuns[0].distanceMeters > 4900)
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

            // 42km marathon simulation
            val route = TestDataFactory.createTestRoute(distanceKm = 42.195)
            val step = route.size / 50 // Sample every 50th point for speed
            for (i in 0 until route.size step step) {
                val routePoint = route[i]
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                    }
                runManager.updateLocation(location)
            }

            val runData = runManager.runData.first()
            runManager.updateRoutePoints(route)

            val heartRateData = TestDataFactory.createHeartRateData(count = 200)

            val endTime = System.currentTimeMillis()
            repository.saveRun(runData, heartRateData, startTime, endTime)

            val savedRuns = repository.getAllRuns().first()
            val savedRun = savedRuns[0]

            // Allow wider margin due to sampling every 50th point
            assertTrue(savedRun.distanceMeters > 35000)
        }
    }

    @Test
    fun multipleConsecutiveRuns_independentTracking() {
        runBlocking {
            // Run 1
            val startTime1 = System.currentTimeMillis()
            runManager.startRun()

            val route1 = TestDataFactory.createTestRoute(distanceKm = 5.0)
            for (routePoint in route1) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
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

            val route2 = TestDataFactory.createTestRoute(distanceKm = 3.0)
            for (routePoint in route2) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
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

            val run1 = allRuns.find { it.distanceMeters > 4900 && it.distanceMeters < 5100 }
            val run2 = allRuns.find { it.distanceMeters > 2900 && it.distanceMeters < 3100 }

            assertTrue(run1 != null && run2 != null)
            assertTrue(run1!!.id != run2!!.id)
        }
    }
}
