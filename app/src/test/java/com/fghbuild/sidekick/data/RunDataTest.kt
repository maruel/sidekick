package com.fghbuild.sidekick.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunDataTest {
    @Test
    fun defaultValues_areCorrect() {
        val runData = RunData()
        assertEquals(0.0, runData.distanceMeters, 0.001)
        assertEquals(0.0, runData.paceMinPerKm, 0.001)
        assertEquals(0L, runData.durationMillis)
        assertTrue(runData.routePoints.isEmpty())
        assertTrue(runData.paceHistory.isEmpty())
        assertFalse(runData.isRunning)
    }

    @Test
    fun copy_preservesOriginalValues() {
        val original =
            RunData(
                distanceMeters = 1000.0,
                paceMinPerKm = 5.0,
                durationMillis = 300000,
                isRunning = true,
            )
        val copy = original.copy()
        assertEquals(original.distanceMeters, copy.distanceMeters, 0.001)
        assertEquals(original.paceMinPerKm, copy.paceMinPerKm, 0.001)
        assertEquals(original.durationMillis, copy.durationMillis)
        assertEquals(original.isRunning, copy.isRunning)
    }

    @Test
    fun copy_canModifySingleField() {
        val original = RunData(distanceMeters = 1000.0, isRunning = true)
        val copy = original.copy(isRunning = false)
        assertEquals(1000.0, copy.distanceMeters, 0.001)
        assertFalse(copy.isRunning)
    }
}
