// Room entity for raw GPS measurements (accuracy, speed, bearing). Can be associated with a run or stored as pre-warmup data.
package com.fghbuild.sidekick.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "gps_measurements",
    foreignKeys = [
        ForeignKey(
            entity = RunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        androidx.room.Index("runId"),
    ],
)
data class GpsMeasurementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // NULL = pre-warmup, NOT NULL = during run
    val runId: Long?,
    // "running", "skiing", NULL = pre-warmup activity unknown
    val activity: String?,
    val timestamp: Long,
    // GPS accuracy estimate in meters
    val accuracy: Float,
    // Bearing accuracy in degrees
    val bearingAccuracy: Float,
    // Speed in m/s
    val speed: Float,
    // Bearing in degrees
    val bearing: Float,
)
