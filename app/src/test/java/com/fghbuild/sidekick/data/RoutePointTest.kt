package com.fghbuild.sidekick.data

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@DisplayName("RoutePoint Tests")
class RoutePointTest {
    @Test
    fun create_storesCorrectValues() {
        val point =
            RoutePoint(
                latitude = 40.7128,
                longitude = -74.0060,
                timestamp = 1234567890L,
            )
        assertEquals(40.7128, point.latitude, 0.0001)
        assertEquals(-74.0060, point.longitude, 0.0001)
        assertEquals(1234567890L, point.timestamp)
    }

    @Test
    fun copy_preservesValues() {
        val original =
            RoutePoint(
                latitude = 40.7128,
                longitude = -74.0060,
                timestamp = 1234567890L,
            )
        val copy = original.copy()
        assertEquals(original.latitude, copy.latitude, 0.0001)
        assertEquals(original.longitude, copy.longitude, 0.0001)
        assertEquals(original.timestamp, copy.timestamp)
    }

    @Test
    fun equality_worksCorrectly() {
        val point1 = RoutePoint(40.7128, -74.0060, 1234567890L)
        val point2 = RoutePoint(40.7128, -74.0060, 1234567890L)
        assertEquals(point1, point2)
    }
}
