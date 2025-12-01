package com.fghbuild.sidekick.util

object HeartRateUtils {
    fun calculateAverageBpm(measurements: List<Int>): Int {
        if (measurements.isEmpty()) return 0
        return measurements.average().toInt()
    }

    fun calculateMaxBpm(measurements: List<Int>): Int {
        return measurements.maxOrNull() ?: 0
    }

    fun calculateMinBpm(measurements: List<Int>): Int {
        return measurements.minOrNull() ?: 0
    }
}
