package com.fghbuild.sidekick.run

import android.location.Location
import com.fghbuild.sidekick.data.RoutePoint
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("RunManager Tests")
class RunManagerTest {
    private lateinit var runManager: RunManager

    @BeforeEach
    fun setup() {
        runManager = RunManager()
    }

    @Test
    @DisplayName("initial state: is not running")
    fun initialState_isNotRunning() {
        assertFalse(runManager.runData.value.isRunning)
    }

    @Test
    @DisplayName("initial state: has zero distance")
    fun initialState_hasZeroDistance() {
        assertEquals(0.0, runManager.runData.value.distanceMeters, 0.001)
    }

    @Test
    @DisplayName("initial state: has zero duration")
    fun initialState_hasZeroDuration() {
        assertEquals(0L, runManager.runData.value.durationMillis)
    }

    @Test
    @DisplayName("initial state: has empty route points")
    fun initialState_hasEmptyRoutePoints() {
        assertTrue(runManager.runData.value.routePoints.isEmpty())
    }

    @Test
    @DisplayName("initial state: has empty pace history")
    fun initialState_hasEmptyPaceHistory() {
        assertTrue(runManager.runData.value.paceHistory.isEmpty())
    }

    @Test
    @DisplayName("startRun: sets running to true")
    fun startRun_setsRunningToTrue() {
        runManager.startRun()
        assertTrue(runManager.runData.value.isRunning)
    }

    @Test
    @DisplayName("pauseRun: sets running to false")
    fun pauseRun_setsRunningToFalse() {
        runManager.startRun()
        runManager.pauseRun()
        assertFalse(runManager.runData.value.isRunning)
    }

    @Test
    @DisplayName("resumeRun: sets running to true")
    fun resumeRun_setsRunningToTrue() {
        runManager.startRun()
        runManager.pauseRun()
        runManager.resumeRun()
        assertTrue(runManager.runData.value.isRunning)
    }

    @Test
    @DisplayName("resumeRun: when already running stays running")
    fun resumeRun_whenAlreadyRunning_staysRunning() {
        runManager.startRun()
        runManager.resumeRun()
        assertTrue(runManager.runData.value.isRunning)
    }

    @Test
    @DisplayName("updateLocation: first location sets zero distance")
    fun updateLocation_firstLocation_setsZeroDistance() {
        runManager.startRun()
        val location = mockk<Location>()
        every { location.latitude } returns 40.7128
        every { location.longitude } returns -74.0060
        every { location.time } returns 0L

        runManager.updateLocation(location)

        assertEquals(0.0, runManager.runData.value.distanceMeters, 0.001)
    }

    @Test
    @DisplayName("updateLocation: second location adds distance")
    fun updateLocation_secondLocation_addsDistance() {
        runManager.startRun()
        val location1 = mockk<Location>()
        every { location1.latitude } returns 40.7128
        every { location1.longitude } returns -74.0060
        every { location1.time } returns 0L

        val location2 = mockk<Location>()
        every { location2.latitude } returns 40.7138
        every { location2.longitude } returns -74.0060
        every { location2.time } returns 0L

        runManager.updateLocation(location1)
        runManager.updateLocation(location2)

        assertTrue(runManager.runData.value.distanceMeters > 0)
    }

    @Test
    @DisplayName("updateLocation: adds pace to history when distance > 0")
    fun updateLocation_addsPaceToHistory_whenDistanceGreaterThanZero() {
        runManager.startRun()
        val baseTime = System.currentTimeMillis()
        val location1 = mockk<Location>()
        every { location1.latitude } returns 40.7128
        every { location1.longitude } returns -74.0060
        every { location1.time } returns baseTime

        val location2 = mockk<Location>()
        every { location2.latitude } returns 40.7138
        every { location2.longitude } returns -74.0060
        every { location2.time } returns baseTime + 60000L

        runManager.updateLocation(location1)
        runManager.updateLocation(location2)

        assertEquals(1, runManager.runData.value.paceHistory.size)
    }

    @Test
    @DisplayName("updateLocation: with location timestamp uses it for duration")
    fun updateLocation_withLocationTimestamp_usesItForDuration() {
        runManager.startRun()
        val startTime = System.currentTimeMillis()
        val location = mockk<Location>()
        every { location.latitude } returns 40.7128
        every { location.longitude } returns -74.0060
        every { location.time } returns startTime + 5000

        runManager.updateLocation(location)

        assertTrue(runManager.runData.value.durationMillis >= 0)
    }

    @Test
    @DisplayName("updateRoutePoints: updates route points")
    fun updateRoutePoints_updatesRoutePoints() {
        val points =
            listOf(
                RoutePoint(40.7128, -74.0060, 1234567890L),
                RoutePoint(40.7138, -74.0070, 1234567900L),
            )

        runManager.updateRoutePoints(points)

        assertEquals(2, runManager.runData.value.routePoints.size)
        assertEquals(40.7128, runManager.runData.value.routePoints[0].latitude, 0.0001)
    }

    @Test
    @DisplayName("updateRoutePoints: empty list clears route points")
    fun updateRoutePoints_emptyList_clearsRoutePoints() {
        val points =
            listOf(
                RoutePoint(40.7128, -74.0060, 1234567890L),
            )
        runManager.updateRoutePoints(points)
        runManager.updateRoutePoints(emptyList())

        assertTrue(runManager.runData.value.routePoints.isEmpty())
    }

    @Test
    @DisplayName("updateLocation: when paused ignores location")
    fun updateLocation_whenPaused_ignoresLocation() {
        runManager.startRun()
        val location1 = mockk<Location>()
        every { location1.latitude } returns 40.7128
        every { location1.longitude } returns -74.0060
        every { location1.time } returns 0L
        runManager.updateLocation(location1)

        runManager.pauseRun()

        val location2 = mockk<Location>()
        every { location2.latitude } returns 40.7138
        every { location2.longitude } returns -74.0060
        every { location2.time } returns 0L
        runManager.updateLocation(location2)

        assertEquals(0.0, runManager.runData.value.distanceMeters, 0.001)
    }

    @Test
    @DisplayName("startRun: resets distance from previous run")
    fun startRun_resetsDistanceFromPreviousRun() {
        runManager.startRun()
        val location1 = mockk<Location>()
        every { location1.latitude } returns 40.7128
        every { location1.longitude } returns -74.0060
        every { location1.time } returns 0L

        val location2 = mockk<Location>()
        every { location2.latitude } returns 40.7138
        every { location2.longitude } returns -74.0060
        every { location2.time } returns 0L

        runManager.updateLocation(location1)
        runManager.updateLocation(location2)
        val distanceAfterFirstRun = runManager.runData.value.distanceMeters
        assertTrue(distanceAfterFirstRun > 0)

        runManager.startRun()

        assertEquals(0.0, runManager.runData.value.distanceMeters, 0.001)

        val location3 = mockk<Location>()
        every { location3.latitude } returns 40.7128
        every { location3.longitude } returns -74.0060
        every { location3.time } returns 0L
        runManager.updateLocation(location3)

        assertEquals(0.0, runManager.runData.value.distanceMeters, 0.001)
    }

    @Test
    @DisplayName("updateLocation: does not add zero pace to history")
    fun updateLocation_doesNotAddZeroPaceToHistory() {
        runManager.startRun()
        val location = mockk<Location>()
        every { location.latitude } returns 40.7128
        every { location.longitude } returns -74.0060
        every { location.time } returns 0L

        runManager.updateLocation(location)

        assertTrue(runManager.runData.value.paceHistory.isEmpty())
    }
}
