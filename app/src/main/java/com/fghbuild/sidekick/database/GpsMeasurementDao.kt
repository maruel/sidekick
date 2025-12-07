package com.fghbuild.sidekick.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface GpsMeasurementDao {
    @Insert
    suspend fun insert(measurement: GpsMeasurementEntity)

    @Query(
        "SELECT * FROM gps_measurements WHERE runId IS NULL AND activity = :activity ORDER BY timestamp",
    )
    suspend fun getPrewarmup(activity: String): List<GpsMeasurementEntity>

    @Query("SELECT * FROM gps_measurements WHERE runId = :runId ORDER BY timestamp")
    suspend fun getRunMeasurements(runId: Long): List<GpsMeasurementEntity>

    @Query(
        "SELECT * FROM gps_measurements WHERE runId IS NULL AND activity = :activity ORDER BY timestamp DESC LIMIT :limit",
    )
    suspend fun getLatestPrewarmup(
        activity: String,
        limit: Int = 100,
    ): List<GpsMeasurementEntity>

    @Query("DELETE FROM gps_measurements WHERE runId IS NULL AND activity = :activity")
    suspend fun deletePrewarmup(activity: String)

    @Query("DELETE FROM gps_measurements WHERE runId = :runId")
    suspend fun deleteForRun(runId: Long)
}
