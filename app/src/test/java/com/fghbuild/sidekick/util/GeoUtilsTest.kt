package com.fghbuild.sidekick.util

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@DisplayName("GeoUtils Tests")
class GeoUtilsTest {
    @Test
    @DisplayName("calculateDistanceMeters: same point returns zero")
    fun calculateDistanceMeters_samePoint_returnsZero() {
        val distance =
            GeoUtils.calculateDistanceMeters(
                lat1 = 40.7128,
                lon1 = -74.0060,
                lat2 = 40.7128,
                lon2 = -74.0060,
            )
        assertEquals(0.0, distance, 0.001)
    }

    @Test
    @DisplayName("calculateDistanceMeters: known distance returns correct value")
    fun calculateDistanceMeters_knownDistance_returnsCorrectValue() {
        val distance =
            GeoUtils.calculateDistanceMeters(
                lat1 = 40.7128,
                lon1 = -74.0060,
                lat2 = 40.7614,
                lon2 = -73.9776,
            )
        assertEquals(5910.0, distance, 100.0)
    }

    @Test
    @DisplayName("calculateDistanceMeters: short distance returns correct value")
    fun calculateDistanceMeters_shortDistance_returnsCorrectValue() {
        val distance =
            GeoUtils.calculateDistanceMeters(
                lat1 = 51.5074,
                lon1 = -0.1278,
                lat2 = 51.5080,
                lon2 = -0.1278,
            )
        assertEquals(66.7, distance, 5.0)
    }

    @Test
    @DisplayName("calculateDistanceMeters: long distance returns correct value")
    fun calculateDistanceMeters_longDistance_returnsCorrectValue() {
        val distance =
            GeoUtils.calculateDistanceMeters(
                lat1 = 51.5074,
                lon1 = -0.1278,
                lat2 = 40.7128,
                lon2 = -74.0060,
            )
        assertEquals(5570000.0, distance, 10000.0)
    }

    @Test
    @DisplayName("calculateDistanceMeters: crossing equator returns correct value")
    fun calculateDistanceMeters_crossingEquator_returnsCorrectValue() {
        val distance =
            GeoUtils.calculateDistanceMeters(
                lat1 = 1.0,
                lon1 = 0.0,
                lat2 = -1.0,
                lon2 = 0.0,
            )
        assertEquals(222000.0, distance, 1000.0)
    }
}
