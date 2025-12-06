package com.fghbuild.sidekick.fixtures

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.fghbuild.sidekick.database.SidekickDatabase
import com.fghbuild.sidekick.repository.RunRepository
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit 4 rule for managing test database lifecycle.
 * Automatically creates a fresh in-memory database for each test.
 *
 * Usage:
 * ```kotlin
 * class MyDatabaseTest {
 *     @get:Rule
 *     val databaseRule = DatabaseTestRule()
 *
 *     @Test
 *     fun myTest() {
 *         val database = databaseRule.database
 *         val repository = databaseRule.repository
 *     }
 * }
 * ```
 */
class DatabaseTestRule : TestRule {
    lateinit var database: SidekickDatabase
    lateinit var repository: RunRepository

    override fun apply(
        base: Statement,
        description: Description,
    ): Statement {
        return object : Statement() {
            override fun evaluate() {
                val appContext = ApplicationProvider.getApplicationContext<Context>()
                database = TestDatabase.createTestDatabase(appContext)
                repository = RunRepository(database.runDao(), database.routePointDao())

                try {
                    base.evaluate()
                } finally {
                    close()
                }
            }
        }
    }

    fun close() {
        if (::database.isInitialized) {
            database.close()
        }
    }
}
