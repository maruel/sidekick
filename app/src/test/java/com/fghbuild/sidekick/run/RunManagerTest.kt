package com.fghbuild.sidekick.run

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
}
