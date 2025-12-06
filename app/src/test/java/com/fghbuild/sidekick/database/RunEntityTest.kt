package com.fghbuild.sidekick.database

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@DisplayName("RunEntity Tests")
class RunEntityTest {
    @Test
    fun create_storesCorrectValues() {
        val entity =
            RunEntity(
                id = 1,
                startTime = 1000L,
                endTime = 2000L,
                distanceMeters = 5000.0,
                durationMillis = 1000L,
                averagePaceMinPerKm = 5.5,
                maxHeartRate = 180,
                minHeartRate = 120,
                averageHeartRate = 150,
            )
        assertEquals(1, entity.id)
        assertEquals(1000L, entity.startTime)
        assertEquals(2000L, entity.endTime)
        assertEquals(5000.0, entity.distanceMeters, 0.001)
        assertEquals(1000L, entity.durationMillis)
        assertEquals(5.5, entity.averagePaceMinPerKm, 0.001)
        assertEquals(180, entity.maxHeartRate)
        assertEquals(120, entity.minHeartRate)
        assertEquals(150, entity.averageHeartRate)
    }

    @Test
    fun defaultId_isZero() {
        val entity =
            RunEntity(
                startTime = 1000L,
                endTime = 2000L,
                distanceMeters = 5000.0,
                durationMillis = 1000L,
                averagePaceMinPerKm = 5.5,
            )
        assertEquals(0, entity.id)
    }

    @Test
    fun defaultHeartRateValues_areZero() {
        val entity =
            RunEntity(
                startTime = 1000L,
                endTime = 2000L,
                distanceMeters = 5000.0,
                durationMillis = 1000L,
                averagePaceMinPerKm = 5.5,
            )
        assertEquals(0, entity.maxHeartRate)
        assertEquals(0, entity.minHeartRate)
        assertEquals(0, entity.averageHeartRate)
    }

    @Test
    fun copy_preservesValues() {
        val original =
            RunEntity(
                id = 1,
                startTime = 1000L,
                endTime = 2000L,
                distanceMeters = 5000.0,
                durationMillis = 1000L,
                averagePaceMinPerKm = 5.5,
            )
        val copy = original.copy()
        assertEquals(original.id, copy.id)
        assertEquals(original.startTime, copy.startTime)
        assertEquals(original.distanceMeters, copy.distanceMeters, 0.001)
    }

    @Test
    fun equality_worksCorrectly() {
        val entity1 =
            RunEntity(
                id = 1,
                startTime = 1000L,
                endTime = 2000L,
                distanceMeters = 5000.0,
                durationMillis = 1000L,
                averagePaceMinPerKm = 5.5,
            )
        val entity2 =
            RunEntity(
                id = 1,
                startTime = 1000L,
                endTime = 2000L,
                distanceMeters = 5000.0,
                durationMillis = 1000L,
                averagePaceMinPerKm = 5.5,
            )
        assertEquals(entity1, entity2)
    }
}
