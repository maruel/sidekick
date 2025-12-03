package com.fghbuild.sidekick.util

data class HeartRateZone(
    val zone: Int,
    val name: String,
    val minBpm: Int,
    val maxBpm: Int,
    val description: String,
)

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

    fun calculateMaxHeartRate(age: Int): Int {
        return 220 - age
    }

    fun getHeartRateZones(age: Int): List<HeartRateZone> {
        val maxHR = calculateMaxHeartRate(age)

        return listOf(
            HeartRateZone(
                zone = 1,
                name = "Zone 1: Rest",
                minBpm = 0,
                maxBpm = (maxHR * 0.50).toInt(),
                description = "Very light recovery",
            ),
            HeartRateZone(
                zone = 2,
                name = "Zone 2: Light",
                minBpm = (maxHR * 0.50).toInt() + 1,
                maxBpm = (maxHR * 0.60).toInt(),
                description = "Warm-up, recovery",
            ),
            HeartRateZone(
                zone = 3,
                name = "Zone 3: Moderate",
                minBpm = (maxHR * 0.60).toInt() + 1,
                maxBpm = (maxHR * 0.70).toInt(),
                description = "Sustainable aerobic",
            ),
            HeartRateZone(
                zone = 4,
                name = "Zone 4: Tempo",
                minBpm = (maxHR * 0.70).toInt() + 1,
                maxBpm = (maxHR * 0.80).toInt(),
                description = "Hard efforts",
            ),
            HeartRateZone(
                zone = 5,
                name = "Zone 5: Max",
                minBpm = (maxHR * 0.80).toInt() + 1,
                maxBpm = maxHR,
                description = "Maximum effort",
            ),
        )
    }

    fun getZoneForBpm(
        bpm: Int,
        age: Int,
    ): HeartRateZone? {
        return getHeartRateZones(age).find { bpm in it.minBpm..it.maxBpm }
    }
}
