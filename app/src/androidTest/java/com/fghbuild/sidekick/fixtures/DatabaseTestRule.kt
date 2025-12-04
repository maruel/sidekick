package com.fghbuild.sidekick.fixtures

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.fghbuild.sidekick.database.SidekickDatabase
import com.fghbuild.sidekick.repository.RunRepository
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * JUnit 5 extension for managing test database lifecycle.
 * Automatically creates a fresh in-memory database for each test.
 *
 * Usage:
 * ```kotlin
 * @ExtendWith(DatabaseTestRule::class)
 * class MyDatabaseTest {
 *     private lateinit var database: SidekickDatabase
 *     private lateinit var repository: RunRepository
 *
 *     @BeforeEach
 *     fun setup(extension: DatabaseTestRule) {
 *         database = extension.database
 *         repository = extension.repository
 *     }
 * }
 * ```
 */
class DatabaseTestRule : BeforeEachCallback {
    lateinit var database: SidekickDatabase
    lateinit var repository: RunRepository

    override fun beforeEach(context: ExtensionContext) {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabase.createTestDatabase(appContext)
        repository = RunRepository(database.runDao(), database.routePointDao())
    }

    fun close() {
        if (::database.isInitialized) {
            database.close()
        }
    }
}
