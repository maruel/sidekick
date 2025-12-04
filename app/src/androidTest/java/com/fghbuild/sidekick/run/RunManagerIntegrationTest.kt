package com.fghbuild.sidekick.run

import android.location.Location
import com.fghbuild.sidekick.fixtures.TestDataFactory
import com.fghbuild.sidekick.util.GeoUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("RunManager Integration Tests")
class RunManagerIntegrationTest {
    private lateinit var runManager: RunManager

    @BeforeEach
    fun setup() {
        runManager = RunManager()
    }

    @Test
    @DisplayName("location tracking: accumulates distance from multiple updates")
    fun locationTracking_accumulatesDistance() =
        runBlocking {
            runManager.startRun()

            val route = TestDataFactory.createTestRoute(distanceKm = 2.0)
            var totalDistance = 0.0

            // Feed locations one by one, tracking accumulated distance
            for (routePoint in route) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                    }
                runManager.updateLocation(location)

                val currentRunData = runManager.runData.first()
                assertTrue(currentRunData.distanceMeters >= totalDistance)
                totalDistance = currentRunData.distanceMeters
            }

            val finalRunData = runManager.runData.first()
            // Should be approximately 2km (within 5% error due to path discretization)
            assertTrue(finalRunData.distanceMeters > 1900)
            assertTrue(finalRunData.distanceMeters < 2100)
        }

    @Test
    @DisplayName("distance calculation: uses Haversine formula")
    fun distanceCalculation_usesHaversine() =
        runBlocking {
            runManager.startRun()

            // San Francisco to nearby point (known distance)
            val sfLat = 37.7749
            val sfLon = -122.4194
            val nearbyLat = 37.7849
            val nearbyLon = -122.4094

            val location1 =
                Location("test").apply {
                    latitude = sfLat
                    longitude = sfLon
                    time = System.currentTimeMillis()
                }
            val location2 =
                Location("test").apply {
                    latitude = nearbyLat
                    longitude = nearbyLon
                    time = System.currentTimeMillis() + 1000
                }

            runManager.updateLocation(location1)
            runManager.updateLocation(location2)

            val runData = runManager.runData.first()
            val expectedDistance =
                GeoUtils.calculateDistanceMeters(
                    sfLat,
                    sfLon,
                    nearbyLat,
                    nearbyLon,
                )

            assertEquals(
                expectedDistance,
                runData.distanceMeters,
                100.0,
            ) // Within 100m
        }

    @Test
    @DisplayName("pace calculation: tracks pace history")
    fun paceCalculation_tracksHistory() =
        runBlocking {
            runManager.startRun()

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

            val finalRunData = runManager.runData.first()
            assertTrue(finalRunData.paceHistory.isNotEmpty())
            assertTrue(finalRunData.paceMinPerKm > 0)
        }

    @Test
    @DisplayName("pause/resume: preserves distance and pace")
    fun pauseResume_preservesData() =
        runBlocking {
            runManager.startRun()

            val route1 = TestDataFactory.createTestRoute(distanceKm = 1.0)
            for (routePoint in route1) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                    }
                runManager.updateLocation(location)
            }

            val runDataBeforePause = runManager.runData.first()
            val distanceBeforePause = runDataBeforePause.distanceMeters
            val paceHistoryBeforePause = runDataBeforePause.paceHistory.size

            runManager.pauseRun()

            val pausedRunData = runManager.runData.first()
            assertEquals(distanceBeforePause, pausedRunData.distanceMeters)
            assertEquals(
                paceHistoryBeforePause,
                pausedRunData.paceHistory.size,
            )

            runManager.resumeRun()

            // Continue with more locations
            val route2 = TestDataFactory.createTestRoute(distanceKm = 1.0)
            for (routePoint in route2) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                    }
                runManager.updateLocation(location)
            }

            val finalRunData = runManager.runData.first()
            // Should have roughly 2km (1+1)
            assertTrue(finalRunData.distanceMeters > 1900)
            assertTrue(finalRunData.distanceMeters < 2100)
        }

    @Test
    @DisplayName("run state: tracks running state correctly")
    fun runState_tracksStateTransitions() =
        runBlocking {
            val initialState = runManager.runData.first()
            assertEquals(false, initialState.isRunning)

            runManager.startRun()
            var runData = runManager.runData.first()
            assertEquals(true, runData.isRunning)

            runManager.pauseRun()
            runData = runManager.runData.first()
            assertEquals(false, runData.isRunning)

            runManager.resumeRun()
            runData = runManager.runData.first()
            assertEquals(true, runData.isRunning)
        }

    @Test
    @DisplayName("route points: updates from location stream")
    fun routePoints_updatesFromLocationStream() =
        runBlocking {
            val testRoute = TestDataFactory.createTestRoute(distanceKm = 1.5)

            runManager.updateRoutePoints(testRoute)

            val runData = runManager.runData.first()
            assertEquals(testRoute.size, runData.routePoints.size)

            for (i in testRoute.indices) {
                assertEquals(
                    testRoute[i].latitude,
                    runData.routePoints[i].latitude,
                    0.000001,
                )
                assertEquals(
                    testRoute[i].longitude,
                    runData.routePoints[i].longitude,
                    0.000001,
                )
            }
        }

    @Test
    @DisplayName("duration: increases with run time")
    fun duration_increasesWithTime() =
        runBlocking {
            runManager.startRun()
            val startTime = System.currentTimeMillis()

            val route = TestDataFactory.createTestRoute(distanceKm = 1.0)
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
            val elapsedTime = System.currentTimeMillis() - startTime

            assertTrue(runData.durationMillis > 0)
            assertTrue(runData.durationMillis <= elapsedTime + 1000)
        }

    @Test
    @DisplayName("pace history: accumulates all pace values")
    fun paceHistory_accumulatesAllValues() =
        runBlocking {
            runManager.startRun()

            val route = TestDataFactory.createTestRoute(distanceKm = 2.0)
            val updateCount = route.size

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
            assertEquals(updateCount, runData.paceHistory.size)

            // All pace values should be positive
            for (pace in runData.paceHistory) {
                assertTrue(pace >= 0)
            }
        }
}
