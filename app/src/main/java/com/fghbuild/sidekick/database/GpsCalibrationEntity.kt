package com.fghbuild.sidekick.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gps_calibration")
data class GpsCalibrationEntity(
    // "running", "skiing"
    @PrimaryKey val activity: String,
    val avgAccuracyMeters: Double,
    val p95AccuracyMeters: Double,
    val avgBearingAccuracyDegrees: Double,
    val samplesCollected: Int,
    val kalmanProcessNoise: Double,
    val kalmanMeasurementNoise: Double,
    val lastUpdated: Long,
)
