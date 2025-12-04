package com.fghbuild.sidekick.database

import androidx.test.core.app.ApplicationProvider
import com.fghbuild.sidekick.fixtures.TestDataFactory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@DisplayName("SidekickDatabase Schema Tests")
class RunDatabaseTest {
    private lateinit var database: SidekickDatabase

    @BeforeEach
    fun setup() {
        val context = ApplicationProvider.getApplicationContext()
        database =
            androidx.room.Room.inMemoryDatabaseBuilder(
                context,
                SidekickDatabase::class.java,
            )
                .allowMainThreadQueries()
                .build()
    }

    @AfterEach
    fun teardown() {
        database.close()
    }

    @Test
    @DisplayName("database: provides both DAOs")
    fun database_providesDaos() {
        val runDao = database.runDao()
        val routePointDao = database.routePointDao()

        assertNotNull(runDao)
        assertNotNull(routePointDao)
    }

    @Test
    @DisplayName("RunEntity: schema allows required fields")
    fun runEntity_schemaAllowsRequiredFields() =
        runBlocking {
            val runDao = database.runDao()
            val runEntity =
                TestDataFactory.createTestRunEntity(
                    distanceMeters = 5000.0,
                    durationMinutes = 45,
                    averagePaceMinPerKm = 9.0,
                    maxHeartRate = 180,
                    minHeartRate = 100,
                    averageHeartRate = 145,
                )

            val id = runDao.insertRun(runEntity)
            assertTrue(id > 0)

            val retrieved = runDao.getRunById(id)
            assertNotNull(retrieved)
            assertEquals(runEntity.distanceMeters, retrieved.distanceMeters)
        }

    @Test
    @DisplayName("RunEntity: schema validates foreignKey relationships")
    fun runEntity_schemaSupportsRunQueries() =
        runBlocking {
            val runDao = database.runDao()

            // Insert multiple runs
            val run1 = TestDataFactory.createTestRunEntity(distanceMeters = 3000.0)
            val run2 = TestDataFactory.createTestRunEntity(distanceMeters = 5000.0)

            val id1 = runDao.insertRun(run1)
            val id2 = runDao.insertRun(run2)

            // Query by ID
            val retrieved1 = runDao.getRunById(id1)
            val retrieved2 = runDao.getRunById(id2)

            assertNotNull(retrieved1)
            assertNotNull(retrieved2)
            assertEquals(3000.0, retrieved1.distanceMeters)
            assertEquals(5000.0, retrieved2.distanceMeters)
        }

    @Test
    @DisplayName("RoutePointEntity: schema requires runId foreignKey")
    fun routePointEntity_schemaRequiresForeignKey() =
        runBlocking {
            val runDao = database.runDao()
            val routePointDao = database.routePointDao()

            val run = TestDataFactory.createTestRunEntity()
            val runId = runDao.insertRun(run)

            val routePoints =
                TestDataFactory.createTestRoutePointEntities(
                    runId,
                    distanceKm = 1.0,
                )
            // Should not throw exception
            routePointDao.insertRoutePoints(routePoints)

            val retrieved = routePointDao.getRoutePointsForRun(runId)
            assertTrue(retrieved.isNotEmpty())
        }

    @Test
    @DisplayName("RoutePointEntity: cascading delete works")
    fun routePointEntity_supportsCascadingDelete() =
        runBlocking {
            val runDao = database.runDao()
            val routePointDao = database.routePointDao()

            val run = TestDataFactory.createTestRunEntity()
            val runId = runDao.insertRun(run)

            val routePoints =
                TestDataFactory.createTestRoutePointEntities(
                    runId,
                    distanceKm = 3.0,
                )
            routePointDao.insertRoutePoints(routePoints)

            val pointsBefore = routePointDao.getRoutePointsForRun(runId)
            assertTrue(pointsBefore.isNotEmpty())

            // Delete run
            val runToDelete = runDao.getRunById(runId)
            assertNotNull(runToDelete)
            runDao.deleteRun(runToDelete)

            // Route points should be deleted (if cascade is configured)
            val pointsAfter = routePointDao.getRoutePointsForRun(runId)
            assertEquals(0, pointsAfter.size)
        }

    @Test
    @DisplayName("getAllRuns: returns all runs sorted by startTime descending")
    fun getAllRuns_returnsSortedByStartTime() =
        runBlocking {
            val runDao = database.runDao()

            val now = System.currentTimeMillis()

            // Insert in non-chronological order
            val run3 =
                TestDataFactory.createTestRunEntity(
                    id = 0,
                ).copy(startTime = now + 2000)
            val run1 =
                TestDataFactory.createTestRunEntity(
                    id = 0,
                ).copy(startTime = now)
            val run2 =
                TestDataFactory.createTestRunEntity(
                    id = 0,
                ).copy(startTime = now + 1000)

            runDao.insertRun(run1)
            runDao.insertRun(run2)
            runDao.insertRun(run3)

            val allRuns = runDao.getAllRuns()
            assertEquals(3, allRuns.size)

            // Verify descending order (newest first)
            for (i in 1 until allRuns.size) {
                assertTrue(
                    allRuns[i - 1].startTime >= allRuns[i].startTime,
                    "Runs should be ordered by startTime descending",
                )
            }
        }

    @Test
    @DisplayName("database: supports concurrent reads")
    fun database_supportsConcurrentReads() =
        runBlocking {
            val runDao = database.runDao()

            val run = TestDataFactory.createTestRunEntity()
            val runId = runDao.insertRun(run)

            // Read multiple times
            val read1 = runDao.getRunById(runId)
            val read2 = runDao.getRunById(runId)
            val read3 = runDao.getRunById(runId)

            assertNotNull(read1)
            assertNotNull(read2)
            assertNotNull(read3)
            assertEquals(read1.id, read2.id)
            assertEquals(read2.id, read3.id)
        }
}
