package com.fghbuild.sidekick.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RunEntity::class, RoutePointEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SidekickDatabase : RoomDatabase() {
    abstract fun runDao(): RunDao

    abstract fun routePointDao(): RoutePointDao

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
                    .build()
                    .also { instance = it }
            }
        }
    }
}
