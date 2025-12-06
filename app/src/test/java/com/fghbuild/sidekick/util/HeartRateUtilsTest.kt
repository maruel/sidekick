package com.fghbuild.sidekick.util

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@DisplayName("HeartRateUtils Tests")
class HeartRateUtilsTest {
    @Test
    fun calculateAverageBpm_emptyList_returnsZero() {
        val average = HeartRateUtils.calculateAverageBpm(emptyList())
        assertEquals(0, average)
    }

    @Test
    fun calculateAverageBpm_singleValue_returnsSameValue() {
        val average = HeartRateUtils.calculateAverageBpm(listOf(150))
        assertEquals(150, average)
    }

    @Test
    fun calculateAverageBpm_multipleValues_returnsAverage() {
        val average = HeartRateUtils.calculateAverageBpm(listOf(140, 150, 160))
        assertEquals(150, average)
    }

    @Test
    fun calculateAverageBpm_roundsDown() {
        val average = HeartRateUtils.calculateAverageBpm(listOf(140, 150, 155))
        assertEquals(148, average)
    }

    @Test
    fun calculateMaxBpm_emptyList_returnsZero() {
        val max = HeartRateUtils.calculateMaxBpm(emptyList())
        assertEquals(0, max)
    }

    @Test
    fun calculateMaxBpm_singleValue_returnsSameValue() {
        val max = HeartRateUtils.calculateMaxBpm(listOf(150))
        assertEquals(150, max)
    }

    @Test
    fun calculateMaxBpm_multipleValues_returnsMax() {
        val max = HeartRateUtils.calculateMaxBpm(listOf(140, 180, 160))
        assertEquals(180, max)
    }

    @Test
    fun calculateMinBpm_emptyList_returnsZero() {
        val min = HeartRateUtils.calculateMinBpm(emptyList())
        assertEquals(0, min)
    }

    @Test
    fun calculateMinBpm_singleValue_returnsSameValue() {
        val min = HeartRateUtils.calculateMinBpm(listOf(150))
        assertEquals(150, min)
    }

    @Test
    fun calculateMinBpm_multipleValues_returnsMin() {
        val min = HeartRateUtils.calculateMinBpm(listOf(140, 180, 160))
        assertEquals(140, min)
    }

    @Test
    fun calculateMaxHeartRate_age30_returns190() {
        val maxHR = HeartRateUtils.calculateMaxHeartRate(30)
        assertEquals(190, maxHR)
    }

    @Test
    fun calculateMaxHeartRate_age50_returns170() {
        val maxHR = HeartRateUtils.calculateMaxHeartRate(50)
        assertEquals(170, maxHR)
    }

    @Test
    fun calculateMaxHeartRate_negativeAge_clampedTo5() {
        val maxHR = HeartRateUtils.calculateMaxHeartRate(-10)
        assertEquals(215, maxHR)
    }

    @Test
    fun calculateMaxHeartRate_extremeAge_clampedTo100() {
        val maxHR = HeartRateUtils.calculateMaxHeartRate(250)
        assertEquals(120, maxHR)
    }

    @Test
    fun getHeartRateZones_returnsFiveZones() {
        val zones = HeartRateUtils.getHeartRateZones(30)
        assertEquals(5, zones.size)
    }

    @Test
    fun getHeartRateZones_zone1StartsAtZero() {
        val zones = HeartRateUtils.getHeartRateZones(30)
        assertEquals(0, zones[0].minBpm)
    }

    @Test
    fun getHeartRateZones_zone5EndsAtMaxHR() {
        val zones = HeartRateUtils.getHeartRateZones(30)
        assertEquals(190, zones[4].maxBpm)
    }

    @Test
    fun getHeartRateZones_zonesHaveCorrectNumbers() {
        val zones = HeartRateUtils.getHeartRateZones(30)
        zones.forEachIndexed { index, zone ->
            assertEquals(index + 1, zone.zone)
        }
    }

    @Test
    fun getZoneForBpm_lowBpm_returnsZone1() {
        val zone = HeartRateUtils.getZoneForBpm(80, 30)
        assertEquals(1, zone?.zone)
    }

    @Test
    fun getZoneForBpm_maxBpm_returnsZone5() {
        val zone = HeartRateUtils.getZoneForBpm(190, 30)
        assertEquals(5, zone?.zone)
    }

    @Test
    fun getZoneForBpm_moderateBpm_returnsZone3() {
        val zone = HeartRateUtils.getZoneForBpm(130, 30)
        assertEquals(3, zone?.zone)
    }

    @Test
    fun getZoneForBpm_aboveMaxHR_returnsNull() {
        val zone = HeartRateUtils.getZoneForBpm(200, 30)
        assertEquals(null, zone)
    }
}
