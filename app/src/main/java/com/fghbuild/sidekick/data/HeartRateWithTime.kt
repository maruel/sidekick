// Represents a single heart rate measurement with its timestamp.
package com.fghbuild.sidekick.data

data class HeartRateWithTime(
    val bpm: Int,
    val timestamp: Long,
)
