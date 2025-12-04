package com.fghbuild.sidekick.fixtures

import android.content.Context
import androidx.room.Room
import com.fghbuild.sidekick.database.SidekickDatabase

/**
 * In-memory database for integration tests.
 * Uses Room testing utilities to create a database that's cleared between tests.
 */
object TestDatabase {
    fun createTestDatabase(context: Context): SidekickDatabase {
        return Room.inMemoryDatabaseBuilder(
            context,
            SidekickDatabase::class.java,
        )
            .allowMainThreadQueries() // Safe for testing
            .build()
    }
}
