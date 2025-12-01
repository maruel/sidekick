package com.fghbuild.sidekick.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val distanceMeters: Double,
    val durationMillis: Long,
    val averagePaceMinPerKm: Double,
    val maxHeartRate: Int = 0,
    val minHeartRate: Int = 0,
    val averageHeartRate: Int = 0,
)
