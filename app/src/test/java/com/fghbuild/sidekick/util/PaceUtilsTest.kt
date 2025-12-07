package com.fghbuild.sidekick.util

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@DisplayName("PaceUtils Tests")
class PaceUtilsTest {
    @Test
    fun calculatePaceMinPerKm_zeroDistance_returnsZero() {
        val pace =
            PaceUtils.calculatePaceMinPerKm(
                durationMillis = 300000,
                distanceMeters = 0.0,
            )
        assertEquals(0.0, pace, 0.001)
    }

    @Test
    fun calculatePaceMinPerKm_negativeDistance_returnsZero() {
        val pace =
            PaceUtils.calculatePaceMinPerKm(
                durationMillis = 300000,
                distanceMeters = -100.0,
            )
        assertEquals(0.0, pace, 0.001)
    }

    @Test
    fun calculatePaceMinPerKm_fiveMinPerKm_returnsCorrectValue() {
        val pace =
            PaceUtils.calculatePaceMinPerKm(
                durationMillis = 5 * 60 * 1000,
                distanceMeters = 1000.0,
            )
        assertEquals(5.0, pace, 0.001)
    }

    @Test
    fun calculatePaceMinPerKm_sixMinPerKm_returnsCorrectValue() {
        val pace =
            PaceUtils.calculatePaceMinPerKm(
                durationMillis = 30 * 60 * 1000,
                distanceMeters = 5000.0,
            )
        assertEquals(6.0, pace, 0.001)
    }

    @Test
    fun formatPace_fiveMinutePace_returnsFormattedString() {
        val formatted = PaceUtils.formatPace(5.0)
        assertEquals("5:00", formatted)
    }

    @Test
    fun formatPace_fiveThirtyPace_returnsFormattedString() {
        val formatted = PaceUtils.formatPace(5.5)
        assertEquals("5:30", formatted)
    }

    @Test
    fun formatPace_zeroPace_returnsZero() {
        val formatted = PaceUtils.formatPace(0.0)
        assertEquals("--", formatted)
    }

    @Test
    fun formatPace_negativePace_returnsZero() {
        val formatted = PaceUtils.formatPace(-5.0)
        assertEquals("--", formatted)
    }

    @Test
    fun formatPace_infinitePace_returnsZero() {
        val formatted = PaceUtils.formatPace(Double.POSITIVE_INFINITY)
        assertEquals("--", formatted)
    }

    @Test
    fun formatDuration_oneHour_returnsFormattedString() {
        val formatted = PaceUtils.formatDuration(3600000)
        assertEquals("1:00:00", formatted)
    }

    @Test
    fun formatDuration_thirtyMinutes_returnsFormattedString() {
        val formatted = PaceUtils.formatDuration(1800000)
        assertEquals("0:30:00", formatted)
    }

    @Test
    fun formatDuration_oneMinuteThirtySeconds_returnsFormattedString() {
        val formatted = PaceUtils.formatDuration(90000)
        assertEquals("0:01:30", formatted)
    }

    @Test
    fun formatDuration_zero_returnsZero() {
        val formatted = PaceUtils.formatDuration(0)
        assertEquals("0:00:00", formatted)
    }
}
