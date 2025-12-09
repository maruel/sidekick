// Data Access Object for GpsCalibration entities. Provides database operations for GPS accuracy calibration parameters.
package com.fghbuild.sidekick.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface GpsCalibrationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(calibration: GpsCalibrationEntity)

    @Update
    suspend fun update(calibration: GpsCalibrationEntity)

    @Query("SELECT * FROM gps_calibration WHERE activity = :activity")
    suspend fun getCalibration(activity: String): GpsCalibrationEntity?

    @Query("SELECT * FROM gps_calibration")
    suspend fun getAllCalibrations(): List<GpsCalibrationEntity>

    @Query("DELETE FROM gps_calibration WHERE activity = :activity")
    suspend fun deleteCalibration(activity: String)
}
