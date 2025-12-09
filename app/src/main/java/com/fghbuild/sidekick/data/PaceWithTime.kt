// Represents a pace measurement (minutes per kilometer) with its timestamp.
package com.fghbuild.sidekick.data

data class PaceWithTime(
    val pace: Double,
    val timestamp: Long,
)
