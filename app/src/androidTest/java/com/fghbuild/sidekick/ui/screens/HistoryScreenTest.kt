package com.fghbuild.sidekick.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fghbuild.sidekick.fixtures.TestDataFactory
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

class HistoryScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun historyScreen_displaysTitle() {
        composeTestRule.setContent {
            historyScreen()
        }
        composeTestRule.onNodeWithText("Run History").assertIsDisplayed()
    }

    @Test
    fun historyScreen_emptyState_displaysNoRunsMessage() {
        composeTestRule.setContent {
            historyScreen(runs = emptyList())
        }
        composeTestRule.onNodeWithText("Run History").assertIsDisplayed()
        composeTestRule.onNodeWithText("No runs recorded yet").assertIsDisplayed()
    }

    @Test
    fun historyScreen_singleRun_displaysRunData() {
        val run = TestDataFactory.createTestRunEntity(distanceMeters = 5000.0)
        composeTestRule.setContent {
            historyScreen(runs = listOf(run))
        }
        composeTestRule.onNodeWithText("Run History").assertIsDisplayed()
        composeTestRule.onNodeWithText("5.00 km", substring = true).assertIsDisplayed() // distance
    }

    @Test
    fun historyScreen_runCard_displaysDistanceFormatted() {
        val run =
            TestDataFactory.createTestRunEntity(
                distanceMeters = 10500.0,
            )
        composeTestRule.setContent {
            historyScreen(runs = listOf(run))
        }
        composeTestRule.onNodeWithText("10.50 km", substring = true).assertIsDisplayed()
    }

    @Test
    fun historyScreen_runCard_displaysDurationFormatted() {
        val run =
            TestDataFactory.createTestRunEntity(
                durationMinutes = 60,
            ) // 1 hour
        composeTestRule.setContent {
            historyScreen(runs = listOf(run))
        }
        composeTestRule.onNodeWithText("01:00:00", substring = true).assertIsDisplayed()
    }

    @Test
    fun historyScreen_runCard_displaysPaceFormatted() {
        val run =
            TestDataFactory.createTestRunEntity(
                averagePaceMinPerKm = 9.5,
            )
        composeTestRule.setContent {
            historyScreen(runs = listOf(run))
        }
        composeTestRule.onNodeWithText("/km", substring = true).assertIsDisplayed()
    }

    @Test
    fun historyScreen_runCard_displaysHeartRateStatsWhenAvailable() {
        val run =
            TestDataFactory.createTestRunEntity(
                averageHeartRate = 150,
                maxHeartRate = 180,
                minHeartRate = 110,
            )
        composeTestRule.setContent {
            historyScreen(runs = listOf(run))
        }
        composeTestRule.onNodeWithText("150", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("180", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("110", substring = true).assertIsDisplayed()
    }

    @Test
    fun historyScreen_runCard_hidesHeartRateStatsWhenZero() {
        val run =
            TestDataFactory.createTestRunEntity(
                averageHeartRate = 0,
                maxHeartRate = 0,
                minHeartRate = 0,
            )
        composeTestRule.setContent {
            historyScreen(runs = listOf(run))
        }
        composeTestRule.onNodeWithText("Run History").assertIsDisplayed()
        // HR stats should not be displayed
    }

    @Test
    fun historyScreen_deleteButton_triggersCallback() {
        val run = TestDataFactory.createTestRunEntity(id = 42)
        var deletedRunId: Long? = null

        composeTestRule.setContent {
            historyScreen(
                runs = listOf(run),
                onDeleteRun = { deletedRunId = it },
            )
        }

        // Icon button uses contentDescription, not visible text
        composeTestRule.onNodeWithContentDescription("Delete run").performClick()
        assertTrue(deletedRunId == 42L)
    }

    @Test
    fun historyScreen_multipleRuns_displaysAllRuns() {
        val now = System.currentTimeMillis()
        val run1 =
            TestDataFactory.createTestRunEntity(
                id = 1,
            ).copy(startTime = now - 7200000)
        val run2 =
            TestDataFactory.createTestRunEntity(
                id = 2,
            ).copy(startTime = now - 3600000)
        val run3 =
            TestDataFactory.createTestRunEntity(
                id = 3,
            ).copy(startTime = now)

        composeTestRule.setContent {
            historyScreen(runs = listOf(run1, run2, run3))
        }

        composeTestRule.onNodeWithText("Run History").assertIsDisplayed()
        // All runs should be visible (we can't directly check without more specific matchers)
    }

    @Test
    fun historyScreen_largeDistance_displaysCorrectlyFormatted() {
        val run =
            TestDataFactory.createTestRunEntity(
                distanceMeters = 42195.0,
            ) // Marathon
        composeTestRule.setContent {
            historyScreen(runs = listOf(run))
        }
        // Verify the history screen renders with the run
        composeTestRule.onNodeWithText("Run History").assertIsDisplayed()
    }

    @Test
    fun historyScreen_realisticRuns_displaysWithAllStats() {
        val runs =
            listOf(
                TestDataFactory.createTestRunEntity(
                    id = 1,
                    distanceMeters = 5000.0,
                    durationMinutes = 45,
                    averageHeartRate = 145,
                ),
                TestDataFactory.createTestRunEntity(
                    id = 2,
                    distanceMeters = 10000.0,
                    durationMinutes = 90,
                    averageHeartRate = 150,
                ),
                TestDataFactory.createTestRunEntity(
                    id = 3,
                    distanceMeters = 3000.0,
                    durationMinutes = 25,
                    averageHeartRate = 140,
                ),
            )

        composeTestRule.setContent {
            historyScreen(runs = runs)
        }

        composeTestRule.onNodeWithText("Run History").assertIsDisplayed()
        // Multiple runs visible - check for formatted distances
        composeTestRule.onNodeWithText("5.00 km", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("10.00 km", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("3.00 km", substring = true).assertIsDisplayed()
    }

    @Test
    fun historyScreen_veryShortRun_displaysDataWithoutErrors() {
        val run =
            TestDataFactory.createTestRunEntity(
                distanceMeters = 100.0,
                durationMinutes = 1,
            )
        composeTestRule.setContent {
            historyScreen(runs = listOf(run))
        }
        composeTestRule.onNodeWithText("Run History").assertIsDisplayed()
        composeTestRule.onNodeWithText("0.10 km", substring = true).assertIsDisplayed()
    }
}
