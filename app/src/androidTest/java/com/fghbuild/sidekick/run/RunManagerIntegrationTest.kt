package com.fghbuild.sidekick.run

import android.location.Location
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fghbuild.sidekick.database.SidekickDatabase
import com.fghbuild.sidekick.fixtures.TestDataFactory
import com.fghbuild.sidekick.util.GeoUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunManagerIntegrationTest {
    private lateinit var runManager: RunManager
    private lateinit var database: SidekickDatabase

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
    fun locationTracking_accumulatesDistance() {
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
    }

    @Test
    fun distanceCalculation_usesHaversine() {
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
    }

    @Test
    fun paceCalculation_tracksHistory() {
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
    }

    @Test
    fun pauseResume_preservesData() {
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
            // Should have roughly 2km (1+1) - allow larger margin for test data accuracy
            assertTrue(finalRunData.distanceMeters > 1500)
            assertTrue(finalRunData.distanceMeters < 2500)
        }
    }

    @Test
    fun runState_tracksStateTransitions() {
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
    }

    @Test
    fun routePoints_updatesFromLocationStream() {
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
    }

    @Test
    fun duration_increasesWithTime() {
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

            // Duration should be tracked (could be based on location timestamps)
            assertTrue(runData.durationMillis >= 0)
            // Allow wider margin due to location timestamp variance
            assertTrue(runData.durationMillis <= elapsedTime + 300000)
        }
    }

    @Test
    fun paceHistory_accumulatesAllValues() {
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
            // First location has no prior point, so pace history has updateCount - 1 entries
            assertEquals(updateCount - 1, runData.paceHistory.size)

            // All pace values should be positive
            for (paceWithTime in runData.paceHistory) {
                assertTrue(paceWithTime.pace >= 0)
            }
        }
    }
}
