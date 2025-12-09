// Room database singleton for Sidekick. Manages all database entities and provides DAOs for data access.
package com.fghbuild.sidekick.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RunEntity::class,
        RoutePointEntity::class,
        GpsMeasurementEntity::class,
        GpsCalibrationEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class SidekickDatabase : RoomDatabase() {
    abstract fun runDao(): RunDao

    abstract fun routePointDao(): RoutePointDao

    abstract fun gpsMeasurementDao(): GpsMeasurementDao

    abstract fun gpsCalibrationDao(): GpsCalibrationDao

    companion object {
        @Volatile
        private var instance: SidekickDatabase? = null

        fun getInstance(context: Context): SidekickDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SidekickDatabase::class.java,
                    "sidekick_database",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
