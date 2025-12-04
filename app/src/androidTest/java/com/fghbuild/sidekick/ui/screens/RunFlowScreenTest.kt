package com.fghbuild.sidekick.ui.screens

import android.location.Location
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fghbuild.sidekick.fixtures.TestDataFactory
import com.fghbuild.sidekick.run.RunManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("Run Flow Screen Tests (Start → InProgress → Save)")
class RunFlowScreenTest {
    private val composeTestRule = createComposeRule()
    private lateinit var runManager: RunManager

    @BeforeEach
    fun setup() {
        runManager = RunManager()
    }

    @Test
    @DisplayName("home screen: start run button transitions to in-progress")
    fun homeScreen_startRunButton_transitionsToInProgress() =
        runBlocking {
            var transitionedToRun = false

            composeTestRule.setContent {
                homeScreen(
                    isRunning = false,
                    onStartRun = {
                        transitionedToRun = true
                        runManager.startRun()
                    },
                )
            }

            composeTestRule.onNodeWithText("Start Run").performClick()
            assertTrue(transitionedToRun)

            val runData = runManager.runData.first()
            assertEquals(true, runData.isRunning)
        }

    @Test
    @DisplayName("in-progress screen: updates with location data")
    fun inProgressScreen_updatesWithLocationData() =
        runBlocking {
            runManager.startRun()

            // Simulate location updates
            val route = TestDataFactory.createTestRoute(distanceKm = 1.0)
            for (routePoint in route.take(10)) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                    }
                runManager.updateLocation(location)
            }

            val runData = runManager.runData.first()
            assertTrue(runData.distanceMeters > 0)
            assertTrue(runData.paceMinPerKm > 0)

            composeTestRule.setContent {
                runInProgressScreen(runData = runData)
            }

            composeTestRule.onNodeWithText("Run in Progress").assertIsDisplayed()
            composeTestRule.onNodeWithText("Distance:", substring = true).assertIsDisplayed()
        }

    @Test
    @DisplayName("in-progress screen: displays heart rate data")
    fun inProgressScreen_displaysHeartRateData() =
        runBlocking {
            runManager.startRun()

            val heartRateData = TestDataFactory.createHeartRateData(count = 20)
            val runData = runManager.runData.first()

            composeTestRule.setContent {
                runInProgressScreen(
                    runData = runData,
                    heartRateData = heartRateData,
                )
            }

            composeTestRule.onNodeWithText("Heart Rate:", substring = true).assertIsDisplayed()
        }

    @Test
    @DisplayName("pause → resume: preserves run data")
    fun pauseResume_preservesRunData() =
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
            assertTrue(finalData.distanceMeters > distanceBeforePause)
        }

    @Test
    @DisplayName("complete flow: start → track → stop → ready to save")
    fun completeFlow_startTrackStopReadyToSave() =
        runBlocking {
            // Home: Start run
            var onStartRunCalled = false
            composeTestRule.setContent {
                homeScreen(
                    isRunning = false,
                    onStartRun = {
                        onStartRunCalled = true
                        runManager.startRun()
                    },
                )
            }

            composeTestRule.onNodeWithText("Start Run").performClick()
            assertTrue(onStartRunCalled)

            val startData = runManager.runData.first()
            assertEquals(true, startData.isRunning)

            // In-Progress: Simulate 3km run
            val route = TestDataFactory.createTestRoute(distanceKm = 3.0)
            for (routePoint in route) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                    }
                runManager.updateLocation(location)
            }

            val inProgressData = runManager.runData.first()
            assertEquals(true, inProgressData.isRunning)
            assertTrue(inProgressData.distanceMeters > 2900)
            assertTrue(inProgressData.distanceMeters < 3100)

            composeTestRule.setContent {
                runInProgressScreen(runData = inProgressData)
            }

            composeTestRule.onNodeWithText("Run in Progress").assertIsDisplayed()

            // In-Progress: Stop run
            var onStopRunCalled = false
            val stopData = inProgressData
            val heartRateData = TestDataFactory.createHeartRateData(count = 50)

            composeTestRule.setContent {
                runInProgressScreen(
                    runData = stopData,
                    heartRateData = heartRateData,
                    onStop = { onStopRunCalled = true },
                )
            }

            composeTestRule.onNodeWithText("Stop").performClick()
            assertTrue(onStopRunCalled)

            // Verify final state is ready to save
            assertEquals(true, stopData.distanceMeters > 0)
            assertEquals(true, stopData.paceMinPerKm > 0)
            assertEquals(true, stopData.routePoints.isNotEmpty())
            assertEquals(true, heartRateData.measurements.isNotEmpty())
        }

    @Test
    @DisplayName("realistic 5km run: full flow")
    fun realistic5kmRun_fullFlow() =
        runBlocking {
            // Start
            runManager.startRun()

            // Track realistic 5km run
            val route = TestDataFactory.createTestRoute(distanceKm = 5.0)
            val heartRateData = TestDataFactory.createHeartRateData(count = 100)

            for (routePoint in route) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                    }
                runManager.updateLocation(location)
            }

            val finalData = runManager.runData.first()

            // Verify run is complete
            assertEquals(true, finalData.distanceMeters > 4900)
            assertEquals(true, finalData.distanceMeters < 5100)
            assertEquals(true, finalData.paceMinPerKm > 0)
            assertEquals(true, finalData.routePoints.isNotEmpty())
            assertEquals(true, heartRateData.measurements.isNotEmpty())

            // Display ready to save
            composeTestRule.setContent {
                runInProgressScreen(
                    runData = finalData,
                    heartRateData = heartRateData,
                )
            }

            composeTestRule.onNodeWithText("Run in Progress").assertIsDisplayed()
            composeTestRule.onNodeWithText("Distance: 5.", substring = true).assertIsDisplayed()
        }

    @Test
    @DisplayName("very short run: handles minimal data")
    fun veryShortRun_handlesMinimalData() =
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
                runInProgressScreen(runData = runData)
            }

            composeTestRule.onNodeWithText("Run in Progress").assertIsDisplayed()
        }

    @Test
    @DisplayName("long run: handles 15km+")
    fun longRun_handles15kmPlus() =
        runBlocking {
            runManager.startRun()

            // 15km run
            val route = TestDataFactory.createTestRoute(distanceKm = 15.0)
            val heartRateData = TestDataFactory.createHeartRateData(count = 150)

            // Just take samples to keep test fast
            val step = route.size / 50 // 50 sample points
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
            assertEquals(true, runData.distanceMeters > 14000)

            composeTestRule.setContent {
                runInProgressScreen(
                    runData = runData,
                    heartRateData = heartRateData,
                )
            }

            composeTestRule.onNodeWithText("Run in Progress").assertIsDisplayed()
        }
}
