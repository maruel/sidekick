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
}
