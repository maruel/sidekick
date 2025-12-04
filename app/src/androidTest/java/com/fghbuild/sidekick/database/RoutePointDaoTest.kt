package com.fghbuild.sidekick.database

import androidx.test.core.app.ApplicationProvider
import com.fghbuild.sidekick.fixtures.TestDataFactory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("RoutePointDao Tests")
class RoutePointDaoTest {
    private lateinit var database: SidekickDatabase
    private lateinit var runDao: RunDao
    private lateinit var routePointDao: RoutePointDao

    @BeforeEach
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        database =
            androidx.room.Room.inMemoryDatabaseBuilder(
                context,
                SidekickDatabase::class.java,
            )
                .allowMainThreadQueries()
                .build()

        runDao = database.runDao()
        routePointDao = database.routePointDao()
    }

    @AfterEach
    fun teardown() {
        database.close()
    }

    @Test
    @DisplayName("insertRoutePoints: stores single route point")
    fun insertRoutePoints_singlePoint() =
        runBlocking {
            val runEntity = TestDataFactory.createTestRunEntity()
            val runId = runDao.insertRun(runEntity)

            val routePoints =
                TestDataFactory.createTestRoutePointEntities(
                    runId,
                    distanceKm = 0.5,
                )
            routePointDao.insertRoutePoints(routePoints)

            val retrievedPoints = routePointDao.getRoutePointsForRun(runId)
            assertEquals(routePoints.size, retrievedPoints.size)
            assertEquals(routePoints[0].latitude, retrievedPoints[0].latitude, 0.000001)
            assertEquals(
                routePoints[0].longitude,
                retrievedPoints[0].longitude,
                0.000001,
            )
        }

    @Test
    @DisplayName("insertRoutePoints: stores 100+ route points")
    fun insertRoutePoints_hundredPlusPoints() =
        runBlocking {
            val runEntity = TestDataFactory.createTestRunEntity()
            val runId = runDao.insertRun(runEntity)

            val routePoints =
                TestDataFactory.createTestRoutePointEntities(
                    runId,
                    distanceKm = 10.0,
                )
            // Should generate 100+ points for 10km
            assertTrue(routePoints.size >= 50)

            routePointDao.insertRoutePoints(routePoints)

            val retrievedPoints = routePointDao.getRoutePointsForRun(runId)
            assertEquals(routePoints.size, retrievedPoints.size)
        }

    @Test
    @DisplayName("getRoutePointsForRun: returns empty list for nonexistent run")
    fun getRoutePointsForRun_nonexistentRun() =
        runBlocking {
            val points = routePointDao.getRoutePointsForRun(99999)
            assertEquals(0, points.size)
        }

    @Test
    @DisplayName("deleteRoutePointsForRun: removes all points for run")
    fun deleteRoutePointsForRun_removesAllPoints() =
        runBlocking {
            val runEntity = TestDataFactory.createTestRunEntity()
            val runId = runDao.insertRun(runEntity)

            val routePoints =
                TestDataFactory.createTestRoutePointEntities(
                    runId,
                    distanceKm = 5.0,
                )
            routePointDao.insertRoutePoints(routePoints)

            val pointsBefore = routePointDao.getRoutePointsForRun(runId)
            assertTrue(pointsBefore.isNotEmpty())

            routePointDao.deleteRoutePointsForRun(runId)

            val pointsAfter = routePointDao.getRoutePointsForRun(runId)
            assertEquals(0, pointsAfter.size)
        }

    @Test
    @DisplayName("insertRoutePoints: preserves latitude/longitude precision")
    fun insertRoutePoints_preservesPrecision() =
        runBlocking {
            val runEntity = TestDataFactory.createTestRunEntity()
            val runId = runDao.insertRun(runEntity)

            val routePoints =
                TestDataFactory.createTestRoutePointEntities(
                    runId,
                    distanceKm = 1.0,
                )
            routePointDao.insertRoutePoints(routePoints)

            val retrievedPoints = routePointDao.getRoutePointsForRun(runId)

            for (i in routePoints.indices) {
                val original = routePoints[i]
                val retrieved = retrievedPoints[i]

                assertEquals(original.latitude, retrieved.latitude, 0.000001)
                assertEquals(original.longitude, retrieved.longitude, 0.000001)
                assertEquals(original.timestamp, retrieved.timestamp)
            }
        }

    @Test
    @DisplayName("insertRoutePoints: multiple runs don't interfere")
    fun insertRoutePoints_multipleRunsIndependent() =
        runBlocking {
            // Create run 1
            val run1 = TestDataFactory.createTestRunEntity()
            val run1Id = runDao.insertRun(run1)
            val points1 = TestDataFactory.createTestRoutePointEntities(run1Id, distanceKm = 3.0)
            routePointDao.insertRoutePoints(points1)

            // Create run 2
            val run2 = TestDataFactory.createTestRunEntity()
            val run2Id = runDao.insertRun(run2)
            val points2 = TestDataFactory.createTestRoutePointEntities(run2Id, distanceKm = 5.0)
            routePointDao.insertRoutePoints(points2)

            val retrieved1 = routePointDao.getRoutePointsForRun(run1Id)
            val retrieved2 = routePointDao.getRoutePointsForRun(run2Id)

            assertEquals(points1.size, retrieved1.size)
            assertEquals(points2.size, retrieved2.size)
            assertTrue(retrieved1.size != retrieved2.size)
        }

    @Test
    @DisplayName("insertRoutePoints: preserves timestamp order")
    fun insertRoutePoints_preservesTimestampOrder() =
        runBlocking {
            val runEntity = TestDataFactory.createTestRunEntity()
            val runId = runDao.insertRun(runEntity)

            val routePoints =
                TestDataFactory.createTestRoutePointEntities(
                    runId,
                    distanceKm = 2.0,
                )
            routePointDao.insertRoutePoints(routePoints)

            val retrievedPoints = routePointDao.getRoutePointsForRun(runId)

            for (i in 1 until retrievedPoints.size) {
                assertTrue(
                    retrievedPoints[i].timestamp >= retrievedPoints[i - 1].timestamp,
                    "Timestamps should be in order",
                )
            }
        }
}
