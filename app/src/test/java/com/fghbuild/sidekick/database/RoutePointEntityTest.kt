package com.fghbuild.sidekick.database

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@DisplayName("RoutePointEntity Tests")
class RoutePointEntityTest {
    @Test
    fun create_storesCorrectValues() {
        val entity =
            RoutePointEntity(
                id = 1,
                runId = 10,
                latitude = 40.7128,
                longitude = -74.0060,
                timestamp = 1234567890L,
            )
        assertEquals(1, entity.id)
        assertEquals(10, entity.runId)
        assertEquals(40.7128, entity.latitude, 0.0001)
        assertEquals(-74.0060, entity.longitude, 0.0001)
        assertEquals(1234567890L, entity.timestamp)
    }

    @Test
    fun defaultId_isZero() {
        val entity =
            RoutePointEntity(
                runId = 10,
                latitude = 40.7128,
                longitude = -74.0060,
                timestamp = 1234567890L,
            )
        assertEquals(0, entity.id)
    }

    @Test
    fun copy_preservesValues() {
        val original =
            RoutePointEntity(
                id = 1,
                runId = 10,
                latitude = 40.7128,
                longitude = -74.0060,
                timestamp = 1234567890L,
            )
        val copy = original.copy()
        assertEquals(original.id, copy.id)
        assertEquals(original.runId, copy.runId)
        assertEquals(original.latitude, copy.latitude, 0.0001)
        assertEquals(original.longitude, copy.longitude, 0.0001)
        assertEquals(original.timestamp, copy.timestamp)
    }

    @Test
    fun copy_canModifyRunId() {
        val original =
            RoutePointEntity(
                id = 1,
                runId = 10,
                latitude = 40.7128,
                longitude = -74.0060,
                timestamp = 1234567890L,
            )
        val copy = original.copy(runId = 20)
        assertEquals(20, copy.runId)
        assertEquals(40.7128, copy.latitude, 0.0001)
    }

    @Test
    fun equality_worksCorrectly() {
        val entity1 = RoutePointEntity(1, 10, 40.7128, -74.0060, 1234567890L)
        val entity2 = RoutePointEntity(1, 10, 40.7128, -74.0060, 1234567890L)
        assertEquals(entity1, entity2)
    }
}
