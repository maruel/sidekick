// Represents current and historical heart rate metrics including BPM and measurements list.
package com.fghbuild.sidekick.data

data class HeartRateData(
    val currentBpm: Int = 0,
    val averageBpm: Int = 0,
    val measurements: List<Int> = emptyList(),
)
