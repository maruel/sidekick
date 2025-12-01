package com.fghbuild.sidekick.run

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RunManagerTest {
    private lateinit var runManager: RunManager

    @Before
    fun setup() {
        runManager = RunManager()
    }

    @Test
    fun initialState_isNotRunning() {
        assertFalse(runManager.runData.value.isRunning)
    }

    @Test
    fun initialState_hasZeroDistance() {
        assertEquals(0.0, runManager.runData.value.distanceMeters, 0.001)
    }

    @Test
    fun initialState_hasZeroDuration() {
        assertEquals(0L, runManager.runData.value.durationMillis)
    }

    @Test
    fun initialState_hasEmptyRoutePoints() {
        assertTrue(runManager.runData.value.routePoints.isEmpty())
    }

    @Test
    fun initialState_hasEmptyPaceHistory() {
        assertTrue(runManager.runData.value.paceHistory.isEmpty())
    }

    @Test
    fun startRun_setsRunningToTrue() {
        runManager.startRun()
        assertTrue(runManager.runData.value.isRunning)
    }

    @Test
    fun pauseRun_setsRunningToFalse() {
        runManager.startRun()
        runManager.pauseRun()
        assertFalse(runManager.runData.value.isRunning)
    }

    @Test
    fun resumeRun_setsRunningToTrue() {
        runManager.startRun()
        runManager.pauseRun()
        runManager.resumeRun()
        assertTrue(runManager.runData.value.isRunning)
    }

    @Test
    fun resumeRun_whenAlreadyRunning_staysRunning() {
        runManager.startRun()
        runManager.resumeRun()
        assertTrue(runManager.runData.value.isRunning)
    }
}
