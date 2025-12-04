package com.fghbuild.sidekick.database

import androidx.test.core.app.ApplicationProvider
import com.fghbuild.sidekick.data.HeartRateData
import com.fghbuild.sidekick.fixtures.TestDataFactory
import com.fghbuild.sidekick.repository.RunRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@DisplayName("RunRepository Integration Tests")
class RunRepositoryTest {
    private lateinit var database: SidekickDatabase
    private lateinit var repository: RunRepository

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

        repository = RunRepository(database.runDao(), database.routePointDao())
    }

    @AfterEach
    fun teardown() {
        database.close()
    }

    @Test
    @DisplayName("saveRun: persists run with empty route")
    fun saveRun_withEmptyRoute() =
        runBlocking {
            val runData = TestDataFactory.createTestRunData(distanceKm = 0.0)
            val heartRateData =
                HeartRateData(
                    currentBpm = 100,
                    averageBpm = 100,
                    measurements = listOf(100),
                )
            val now = System.currentTimeMillis()

            repository.saveRun(runData, heartRateData, now, now + 60000)

            val savedRuns = repository.getAllRuns().first()
            assertEquals(1, savedRuns.size)

            val savedRun = savedRuns[0]
            assertEquals(0.0, savedRun.distanceMeters, 0.01)
            assertEquals(100, savedRun.averageHeartRate)
        }

    @Test
    @DisplayName("saveRun: persists run with route points")
    fun saveRun_withRoutePoints() =
        runBlocking {
            val runData = TestDataFactory.createTestRunData(distanceKm = 5.0)
            val heartRateData =
                HeartRateData(
                    currentBpm = 150,
                    averageBpm = 145,
                    measurements = TestDataFactory.createHeartRateData(50).measurements,
                )
            val now = System.currentTimeMillis()

            repository.saveRun(runData, heartRateData, now, now + 2700000)

            val savedRuns = repository.getAllRuns().first()
            assertEquals(1, savedRuns.size)

            val savedRun = savedRuns[0]
            assertEquals(5000.0, savedRun.distanceMeters, 100.0)
            assertEquals(145, savedRun.averageHeartRate)

            val routePoints = repository.getRoutePointsForRun(savedRun.id)
            assertTrue(routePoints.isNotEmpty())
        }

    @Test
    @DisplayName("saveRun: persists run with 100+ route points")
    fun saveRun_with100PlusRoutePoints() =
        runBlocking {
            val runData =
                TestDataFactory.createTestRunData(distanceKm = 10.0)
            val heartRateData = TestDataFactory.createHeartRateData(150)
            val now = System.currentTimeMillis()

            repository.saveRun(
                runData,
                heartRateData,
                now,
                now + 3600000,
            )

            val savedRuns = repository.getAllRuns().first()
            val savedRun = savedRuns[0]

            val routePoints = repository.getRoutePointsForRun(savedRun.id)
            assertTrue(routePoints.size >= 50) // Should have many points for 10km
        }

    @Test
    @DisplayName("getAllRuns: returns runs ordered by date (newest first)")
    fun getAllRuns_returnsSortedByDate() =
        runBlocking {
            val now = System.currentTimeMillis()

            // Save run 1 (older)
            val run1Data = TestDataFactory.createTestRunData(distanceKm = 3.0)
            val hr1 = HeartRateData(currentBpm = 140, averageBpm = 140)
            repository.saveRun(run1Data, hr1, now - 7200000, now - 3600000)

            // Save run 2 (newer)
            val run2Data = TestDataFactory.createTestRunData(distanceKm = 5.0)
            val hr2 = HeartRateData(currentBpm = 150, averageBpm = 150)
            repository.saveRun(run2Data, hr2, now, now + 1800000)

            val runs = repository.getAllRuns().first()
            assertEquals(2, runs.size)

            // Verify newest is first (higher startTime)
            assertTrue(runs[0].startTime > runs[1].startTime)
        }

    @Test
    @DisplayName("getRoutePointsForRun: returns points for specific run")
    fun getRoutePointsForRun_returnsSpecificRunPoints() =
        runBlocking {
            val now = System.currentTimeMillis()

            // Save run 1
            val run1Data = TestDataFactory.createTestRunData(distanceKm = 5.0)
            val hr1 = HeartRateData(currentBpm = 140, averageBpm = 140)
            repository.saveRun(run1Data, hr1, now, now + 1800000)

            // Save run 2
            val run2Data = TestDataFactory.createTestRunData(distanceKm = 3.0)
            val hr2 = HeartRateData(currentBpm = 150, averageBpm = 150)
            repository.saveRun(run2Data, hr2, now + 3600000, now + 5400000)

            val runs = repository.getAllRuns().first()
            val run1Id = runs[1].id // First saved run
            val run2Id = runs[0].id // Second saved run

            val run1Points = repository.getRoutePointsForRun(run1Id)
            val run2Points = repository.getRoutePointsForRun(run2Id)

            assertTrue(run1Points.isNotEmpty())
            assertTrue(run2Points.isNotEmpty())
            // Runs should have different number of points (different distances)
            assertTrue(run1Points.size != run2Points.size)
        }

    @Test
    @DisplayName("deleteRun: removes run and cascade deletes route points")
    fun deleteRun_cascadeDeletesRoutePoints() =
        runBlocking {
            val now = System.currentTimeMillis()
            val runData = TestDataFactory.createTestRunData(distanceKm = 5.0)
            val heartRateData = TestDataFactory.createHeartRateData(100)

            repository.saveRun(runData, heartRateData, now, now + 1800000)

            val runsBeforeDelete = repository.getAllRuns().first()
            assertEquals(1, runsBeforeDelete.size)

            val runId = runsBeforeDelete[0].id
            val routePointsBefore = repository.getRoutePointsForRun(runId)
            assertTrue(routePointsBefore.isNotEmpty())

            repository.deleteRun(runId)

            val runsAfterDelete = repository.getAllRuns().first()
            assertEquals(0, runsAfterDelete.size)

            val routePointsAfter = repository.getRoutePointsForRun(runId)
            assertEquals(0, routePointsAfter.size)
        }

    @Test
    @DisplayName("saveRun: calculates average pace correctly")
    fun saveRun_calculatesAveragePaceCorrectly() =
        runBlocking {
            val runData =
                TestDataFactory.createTestRunData(
                    distanceKm = 5.0,
                    durationMinutes = 45,
                )
            val heartRateData = TestDataFactory.createHeartRateData(100)
            val now = System.currentTimeMillis()

            repository.saveRun(
                runData,
                heartRateData,
                now,
                now + 2700000,
            ) // 45 minutes

            val runs = repository.getAllRuns().first()
            val savedRun = runs[0]

            assertNotNull(savedRun.averagePaceMinPerKm)
            assertTrue(
                savedRun.averagePaceMinPerKm in 8.0..10.0,
            ) // Expected pace for 5km in 45min
        }

    @Test
    @DisplayName("saveRun: persists heart rate statistics")
    fun saveRun_persistsHeartRateStatistics() =
        runBlocking {
            val measurements = listOf(100, 120, 140, 160, 180, 170, 150, 130, 120, 110)
            val heartRateData =
                HeartRateData(
                    currentBpm = 110,
                    averageBpm = 138,
                    measurements = measurements,
                )
            val runData = TestDataFactory.createTestRunData(distanceKm = 5.0)
            val now = System.currentTimeMillis()

            repository.saveRun(runData, heartRateData, now, now + 1800000)

            val runs = repository.getAllRuns().first()
            val savedRun = runs[0]

            assertEquals(180, savedRun.maxHeartRate)
            assertEquals(100, savedRun.minHeartRate)
            assertEquals(138, savedRun.averageHeartRate)
        }
}
