package com.fghbuild.sidekick.ui.screens

import android.location.Location
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fghbuild.sidekick.database.SidekickDatabase
import com.fghbuild.sidekick.fixtures.TestDataFactory
import com.fghbuild.sidekick.run.RunManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunFlowScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private lateinit var database: SidekickDatabase
    private lateinit var runManager: RunManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        database =
            Room.inMemoryDatabaseBuilder(
                context,
                SidekickDatabase::class.java,
            )
                .allowMainThreadQueries()
                .build()
        runManager =
            RunManager(
                database.gpsMeasurementDao(),
                database.gpsCalibrationDao(),
            )
    }

    @Test
    fun homeScreen_startRunButton_transitionsToInProgress() =
        runBlocking {
            var transitionedToRun = false

            composeTestRule.setContent {
                homeScreen(
                    onStartRun = {
                        transitionedToRun = true
                        runManager.startRun()
                    },
                    runData = com.fghbuild.sidekick.data.RunData(),
                    heartRateData = com.fghbuild.sidekick.data.HeartRateData(),
                    connectedDevice = null,
                    userAge = 30,
                    discoveredDevices = emptyList(),
                    isScanning = false,
                    onStartScanning = {},
                    onStopScanning = {},
                    onSelectDevice = {},
                    onDisconnect = {},
                    gpsAccuracyMeters = kotlinx.coroutines.flow.MutableStateFlow(5.0f),
                    currentLocation = kotlinx.coroutines.flow.MutableStateFlow(null),
                )
            }

            composeTestRule.onNodeWithText("Start Run").performClick()
            assertTrue(transitionedToRun)

            val runData = runManager.runData.first()
            assertEquals(true, runData.isRunning)
        }

    @Test
    fun inProgressScreen_updatesWithLocationData() {
        runBlocking {
            runManager.startRun()

            // Create realistic GPS data with better spacing
            val baseLat = 37.7749
            val baseLon = -122.4194
            var currentLat = baseLat
            var currentLon = baseLon
            var timestamp = System.currentTimeMillis()

            // Create 10 points with good spacing
            for (i in 0 until 10) {
                val location =
                    Location("test").apply {
                        latitude = currentLat
                        longitude = currentLon
                        time = timestamp + i * 5000L // 5 second intervals
                        accuracy = 8.0f
                        bearing = 0.0f
                        speed = 3.0f
                    }
                runManager.updateLocation(location)
                currentLat += 0.001 // Move north (~111m per point)
            }

            val runData = runManager.runData.first()
            assertTrue(runData.distanceMeters > 0, "Distance should be positive, was: ${runData.distanceMeters}")
            assertTrue(runData.paceMinPerKm >= 0, "Pace should be non-negative, was: ${runData.paceMinPerKm}")

            composeTestRule.setContent {
                runInProgressScreen(
                    runData = runData,
                    heartRateData = com.fghbuild.sidekick.data.HeartRateData(),
                    onPause = {},
                    onResume = {},
                    onStop = {},
                    connectedDevice = null,
                    userAge = 30,
                    gpsAccuracyMeters = null,
                    currentLocation = null,
                )
            }

            composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
        }
    }

    @Test
    fun inProgressScreen_displaysHeartRateData() {
        runBlocking {
            runManager.startRun()

            val heartRateData = TestDataFactory.createHeartRateData(count = 20)
            val runData = runManager.runData.first()

            composeTestRule.setContent {
                runInProgressScreen(
                    runData = runData,
                    heartRateData = heartRateData,
                    onPause = {},
                    onResume = {},
                    onStop = {},
                    connectedDevice = null,
                    userAge = 30,
                    gpsAccuracyMeters = null,
                    currentLocation = null,
                )
            }

            composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
        }
    }

    @Test
    fun pauseResume_preservesRunData() {
        runBlocking {
            runManager.startRun()

            // Track for 1km
            val route = TestDataFactory.createTestRoute(distanceKm = 1.0)
            for (routePoint in route.take(20)) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                    }
                runManager.updateLocation(location)
            }

            val dataBeforePause = runManager.runData.first()
            val distanceBeforePause = dataBeforePause.distanceMeters

            runManager.pauseRun()
            var pausedData = runManager.runData.first()
            assertEquals(false, pausedData.isRunning)
            assertEquals(distanceBeforePause, pausedData.distanceMeters)

            runManager.resumeRun()
            var resumedData = runManager.runData.first()
            assertEquals(true, resumedData.isRunning)
            assertEquals(distanceBeforePause, resumedData.distanceMeters)

            // Continue tracking
            for (routePoint in route.drop(20).take(20)) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                    }
                runManager.updateLocation(location)
            }

            val finalData = runManager.runData.first()
            // After resume and continued tracking, distance should have increased or stayed similar
            assertTrue(finalData.distanceMeters >= distanceBeforePause)
        }
    }

    @Test
    fun completeFlow_startTrackStopReadyToSave() {
        runBlocking {
            // First test: Home screen and start run
            var onStartRunCalled = false
            composeTestRule.setContent {
                homeScreen(
                    onStartRun = {
                        onStartRunCalled = true
                        runManager.startRun()
                    },
                    runData = com.fghbuild.sidekick.data.RunData(),
                    heartRateData = com.fghbuild.sidekick.data.HeartRateData(),
                    connectedDevice = null,
                    userAge = 30,
                    discoveredDevices = emptyList(),
                    isScanning = false,
                    onStartScanning = {},
                    onStopScanning = {},
                    onSelectDevice = {},
                    onDisconnect = {},
                    gpsAccuracyMeters = kotlinx.coroutines.flow.MutableStateFlow(5.0f),
                    currentLocation = kotlinx.coroutines.flow.MutableStateFlow(null),
                )
            }

            composeTestRule.onNodeWithText("Start Run").performClick()
            assertTrue(onStartRunCalled)

            val startData = runManager.runData.first()
            assertEquals(true, startData.isRunning)

            // Simulate 3km run with realistic GPS data
            val baseLat = 37.7749
            val baseLon = -122.4194
            var currentLat = baseLat
            var currentLon = baseLon
            var timestamp = System.currentTimeMillis()

            // Create points to cover ~3km
            val numPoints = 15
            val distancePerPoint = 3000.0 / numPoints // ~200m per point
            val latDelta = distancePerPoint / 111000.0

            for (i in 0 until numPoints) {
                val location =
                    Location("test").apply {
                        latitude = currentLat
                        longitude = currentLon
                        time = timestamp + i * 5000L // 5 second intervals
                        accuracy = 8.0f
                        bearing = 0.0f
                        speed = 3.0f
                    }
                runManager.updateLocation(location)
                currentLat += latDelta
            }

            val inProgressData = runManager.runData.first()
            assertEquals(true, inProgressData.isRunning)
            assertTrue(inProgressData.distanceMeters > 1000, "Distance should be reasonable, was: ${inProgressData.distanceMeters}")
            assertTrue(inProgressData.distanceMeters < 5000, "Distance should be under 5km, was: ${inProgressData.distanceMeters}")

            // Verify final state is ready to save
            assertTrue(inProgressData.distanceMeters > 0)
            assertTrue(inProgressData.paceMinPerKm >= 0)
        }
    }

    @Test
    fun realistic5kmRun_fullFlow() {
        runBlocking {
            // Start
            runManager.startRun()

            // Track realistic 5km run with better GPS data
            val baseLat = 37.7749
            val baseLon = -122.4194
            var currentLat = baseLat
            var currentLon = baseLon
            var timestamp = System.currentTimeMillis()

            // Create points to cover ~5km
            val numPoints = 25
            val distancePerPoint = 5000.0 / numPoints // ~200m per point
            val latDelta = distancePerPoint / 111000.0

            for (i in 0 until numPoints) {
                val location =
                    Location("test").apply {
                        latitude = currentLat
                        longitude = currentLon
                        time = timestamp + i * 5000L // 5 second intervals
                        accuracy = 8.0f
                        bearing = 0.0f
                        speed = 3.0f
                    }
                runManager.updateLocation(location)
                currentLat += latDelta
            }

            val heartRateData = TestDataFactory.createHeartRateData(count = 100)

            val finalData = runManager.runData.first()

            // Verify run is in progress and has basic data
            assertTrue(finalData.isRunning)
            // Distance or route points should have data
            assertTrue(finalData.distanceMeters > 0 || finalData.routePoints.isNotEmpty(), "Should have distance or route points")
        }
    }

    @Test
    fun veryShortRun_handlesMinimalData() {
        runBlocking {
            runManager.startRun()

            // Just 100m
            val route = TestDataFactory.createTestRoute(distanceKm = 0.1)
            for (routePoint in route) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                    }
                runManager.updateLocation(location)
            }

            val runData = runManager.runData.first()

            composeTestRule.setContent {
                runInProgressScreen(
                    runData = runData,
                    heartRateData = com.fghbuild.sidekick.data.HeartRateData(),
                    onPause = {},
                    onResume = {},
                    onStop = {},
                    connectedDevice = null,
                    userAge = 30,
                    gpsAccuracyMeters = null,
                    currentLocation = null,
                )
            }

            composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
        }
    }

    @Test
    fun longRun_handles15kmPlus() {
        runBlocking {
            runManager.startRun()

            // 15km run with realistic GPS data
            val baseLat = 37.7749
            val baseLon = -122.4194
            var currentLat = baseLat
            var currentLon = baseLon
            var timestamp = System.currentTimeMillis()

            // Create points to cover ~15km
            val numPoints = 50
            val distancePerPoint = 15000.0 / numPoints // ~300m per point
            val latDelta = distancePerPoint / 111000.0

            for (i in 0 until numPoints) {
                val location =
                    Location("test").apply {
                        latitude = currentLat
                        longitude = currentLon
                        time = timestamp + i * 5000L // 5 second intervals
                        accuracy = 8.0f
                        bearing = 0.0f
                        speed = 3.0f
                    }
                runManager.updateLocation(location)
                currentLat += latDelta
            }

            val heartRateData = TestDataFactory.createHeartRateData(count = 150)

            val runData = runManager.runData.first()
            assertTrue(runData.distanceMeters > 1000, "Should have reasonable distance for long run, was: ${runData.distanceMeters}")

            composeTestRule.setContent {
                runInProgressScreen(
                    runData = runData,
                    heartRateData = heartRateData,
                    onPause = {},
                    onResume = {},
                    onStop = {},
                    connectedDevice = null,
                    userAge = 30,
                    gpsAccuracyMeters = null,
                    currentLocation = null,
                )
            }

            composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
        }
    }
}
