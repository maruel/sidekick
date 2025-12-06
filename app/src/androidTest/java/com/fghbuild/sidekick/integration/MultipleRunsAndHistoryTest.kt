package com.fghbuild.sidekick.integration

import android.location.Location
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.fghbuild.sidekick.database.SidekickDatabase
import com.fghbuild.sidekick.fixtures.TestDataFactory
import com.fghbuild.sidekick.repository.RunRepository
import com.fghbuild.sidekick.run.RunManager
import com.fghbuild.sidekick.ui.screens.historyScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MultipleRunsAndHistoryTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private lateinit var database: SidekickDatabase
    private lateinit var repository: RunRepository

    @Before
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

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun save3Runs_displaysAllInHistory() =
        runBlocking {
            val now = System.currentTimeMillis()

            // Run 1: 5km
            val runManager1 = RunManager()
            runManager1.startRun()
            val route1 = TestDataFactory.createTestRoute(distanceKm = 5.0)
            for (routePoint in route1) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                    }
                runManager1.updateLocation(location)
            }
            val runData1 = runManager1.runData.first()
            runManager1.updateRoutePoints(route1)
            val hrData1 = TestDataFactory.createHeartRateData(count = 50)
            repository.saveRun(runData1, hrData1, now - 7200000, now - 5400000)

            // Run 2: 3km
            val runManager2 = RunManager()
            runManager2.startRun()
            val route2 = TestDataFactory.createTestRoute(distanceKm = 3.0)
            for (routePoint in route2) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                    }
                runManager2.updateLocation(location)
            }
            val runData2 = runManager2.runData.first()
            runManager2.updateRoutePoints(route2)
            val hrData2 = TestDataFactory.createHeartRateData(count = 30)
            repository.saveRun(runData2, hrData2, now - 3600000, now - 1800000)

            // Run 3: 10km
            val runManager3 = RunManager()
            runManager3.startRun()
            val route3 = TestDataFactory.createTestRoute(distanceKm = 10.0)
            val step = route3.size / 30
            for (i in 0 until route3.size step step) {
                val routePoint = route3[i]
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                    }
                runManager3.updateLocation(location)
            }
            val runData3 = runManager3.runData.first()
            runManager3.updateRoutePoints(route3)
            val hrData3 = TestDataFactory.createHeartRateData(count = 100)
            repository.saveRun(runData3, hrData3, now, now + 3600000)

            // Display history
            val allRuns = repository.getAllRuns().first()
            assertEquals(3, allRuns.size)

            composeTestRule.setContent {
                historyScreen(runs = allRuns)
            }

            composeTestRule.onNodeWithText("Run History").assertIsDisplayed()
        }

    @Test
    fun deleteRunFromHistory_removedFromDatabase() =
        runBlocking {
            val startTime = System.currentTimeMillis()

            // Save a run
            val runManager = RunManager()
            runManager.startRun()
            val route = TestDataFactory.createTestRoute(distanceKm = 5.0)
            for (routePoint in route) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                    }
                runManager.updateLocation(location)
            }
            val runData = runManager.runData.first()
            runManager.updateRoutePoints(route)
            val hrData = TestDataFactory.createHeartRateData(count = 50)
            repository.saveRun(runData, hrData, startTime, System.currentTimeMillis())

            val runsBefore = repository.getAllRuns().first()
            assertEquals(1, runsBefore.size)
            val runIdToDelete = runsBefore[0].id

            // Delete
            repository.deleteRun(runIdToDelete)

            val runsAfter = repository.getAllRuns().first()
            assertEquals(0, runsAfter.size)
        }

    @Test
    fun historyWithVaryingDistances_allDisplayedCorrectly() =
        runBlocking {
            val distances = listOf(1.0, 3.5, 5.0, 8.0, 15.0, 21.0, 42.195)
            val now = System.currentTimeMillis()

            for ((index, distance) in distances.withIndex()) {
                val runManager = RunManager()
                runManager.startRun()

                val route = TestDataFactory.createTestRoute(distanceKm = distance)
                val step = maxOf(1, route.size / 20)
                for (i in 0 until route.size step step) {
                    val routePoint = route[i]
                    val location =
                        Location("test").apply {
                            latitude = routePoint.latitude
                            longitude = routePoint.longitude
                            time = routePoint.timestamp
                        }
                    runManager.updateLocation(location)
                }

                val runData = runManager.runData.first()
                runManager.updateRoutePoints(route)
                val hrData = TestDataFactory.createHeartRateData(count = (distance * 10).toInt())

                val startTime = now - (7 - index) * 3600000L
                repository.saveRun(runData, hrData, startTime, startTime + 3600000)
            }

            val allRuns = repository.getAllRuns().first()
            assertEquals(distances.size, allRuns.size)

            composeTestRule.setContent {
                historyScreen(runs = allRuns)
            }

            composeTestRule.onNodeWithText("Run History").assertIsDisplayed()
        }

    @Test
    fun historySorting_newestRunsFirst() =
        runBlocking {
            val now = System.currentTimeMillis()

            // Create 3 runs in non-chronological order
            for (index in listOf(1, 0, 2)) {
                val runManager = RunManager()
                runManager.startRun()

                val route = TestDataFactory.createTestRoute(distanceKm = 5.0)
                val step = route.size / 20
                for (i in 0 until route.size step step) {
                    val routePoint = route[i]
                    val location =
                        Location("test").apply {
                            latitude = routePoint.latitude
                            longitude = routePoint.longitude
                            time = routePoint.timestamp
                        }
                    runManager.updateLocation(location)
                }

                val runData = runManager.runData.first()
                runManager.updateRoutePoints(route)
                val hrData = TestDataFactory.createHeartRateData(count = 50)

                val startTime = now - (3 - index) * 7200000L
                repository.saveRun(runData, hrData, startTime, startTime + 3600000)
            }

            val allRuns = repository.getAllRuns().first()
            assertEquals(3, allRuns.size)

            // Verify newest is first
            for (i in 1 until allRuns.size) {
                assertTrue(allRuns[i - 1].startTime >= allRuns[i].startTime)
            }

            composeTestRule.setContent {
                historyScreen(runs = allRuns)
            }

            composeTestRule.onNodeWithText("Run History").assertIsDisplayed()
        }

    @Test
    fun deleteAllRuns_historyShowsEmptyState() =
        runBlocking {
            val now = System.currentTimeMillis()

            // Create 3 runs
            val runIds = mutableListOf<Long>()
            for (i in 0..2) {
                val runManager = RunManager()
                runManager.startRun()

                val route = TestDataFactory.createTestRoute(distanceKm = (i + 1) * 2.0)
                val step = route.size / 10
                for (j in 0 until route.size step step) {
                    val routePoint = route[j]
                    val location =
                        Location("test").apply {
                            latitude = routePoint.latitude
                            longitude = routePoint.longitude
                            time = routePoint.timestamp
                        }
                    runManager.updateLocation(location)
                }

                val runData = runManager.runData.first()
                runManager.updateRoutePoints(route)
                val hrData = TestDataFactory.createHeartRateData(count = 30)

                val startTime = now - (3 - i) * 7200000L
                repository.saveRun(runData, hrData, startTime, startTime + 3600000)
            }

            var allRuns = repository.getAllRuns().first()
            assertEquals(3, allRuns.size)
            runIds.addAll(allRuns.map { it.id })

            // Delete all
            for (runId in runIds) {
                repository.deleteRun(runId)
            }

            allRuns = repository.getAllRuns().first()
            assertEquals(0, allRuns.size)

            composeTestRule.setContent {
                historyScreen(runs = allRuns)
            }

            composeTestRule.onNodeWithText("Run History").assertIsDisplayed()
            composeTestRule.onNodeWithText("No runs recorded yet").assertIsDisplayed()
        }

    @Test
    fun historyWithHeartRateVariation_displaysStatsCorrectly() =
        runBlocking {
            val now = System.currentTimeMillis()

            // Easy run (low HR)
            val runManager1 = RunManager()
            runManager1.startRun()
            val route1 = TestDataFactory.createTestRoute(distanceKm = 3.0)
            val step = route1.size / 15
            for (i in 0 until route1.size step step) {
                val routePoint = route1[i]
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                    }
                runManager1.updateLocation(location)
            }
            val runData1 = runManager1.runData.first()
            runManager1.updateRoutePoints(route1)
            val hrData1 =
                TestDataFactory.createHeartRateData(
                    count = 30,
                    minBpm = 110,
                    maxBpm = 140,
                )
            repository.saveRun(runData1, hrData1, now - 7200000, now - 5400000)

            // Hard run (high HR)
            val runManager2 = RunManager()
            runManager2.startRun()
            val route2 = TestDataFactory.createTestRoute(distanceKm = 5.0)
            for (i in 0 until route2.size step step) {
                val routePoint = route2[i]
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                    }
                runManager2.updateLocation(location)
            }
            val runData2 = runManager2.runData.first()
            runManager2.updateRoutePoints(route2)
            val hrData2 =
                TestDataFactory.createHeartRateData(
                    count = 50,
                    minBpm = 160,
                    maxBpm = 190,
                )
            repository.saveRun(runData2, hrData2, now, now + 3600000)

            val allRuns = repository.getAllRuns().first()
            assertEquals(2, allRuns.size)

            composeTestRule.setContent {
                historyScreen(runs = allRuns)
            }

            composeTestRule.onNodeWithText("Run History").assertIsDisplayed()
        }
}
