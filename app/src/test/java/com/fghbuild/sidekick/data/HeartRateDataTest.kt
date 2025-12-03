package com.fghbuild.sidekick.data

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("HeartRateData Tests")
class HeartRateDataTest {
    @Test
    fun defaultValues_areCorrect() {
        val hrData = HeartRateData()
        assertEquals(0, hrData.currentBpm)
        assertEquals(0, hrData.averageBpm)
        assertTrue(hrData.measurements.isEmpty())
    }

    @Test
    fun copy_preservesOriginalValues() {
        val original =
            HeartRateData(
                currentBpm = 150,
                averageBpm = 145,
                measurements = listOf(140, 145, 150),
            )
        val copy = original.copy()
        assertEquals(original.currentBpm, copy.currentBpm)
        assertEquals(original.averageBpm, copy.averageBpm)
        assertEquals(original.measurements, copy.measurements)
    }

    @Test
    fun copy_canAddMeasurement() {
        val original =
            HeartRateData(
                currentBpm = 150,
                measurements = listOf(140, 150),
            )
        val copy =
            original.copy(
                currentBpm = 160,
                measurements = original.measurements + 160,
            )
        assertEquals(160, copy.currentBpm)
        assertEquals(3, copy.measurements.size)
        assertEquals(160, copy.measurements.last())
    }
}
