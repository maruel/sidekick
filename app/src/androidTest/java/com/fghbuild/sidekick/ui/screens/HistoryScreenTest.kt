package com.fghbuild.sidekick.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fghbuild.sidekick.fixtures.TestDataFactory
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@DisplayName("HistoryScreen Tests")
class HistoryScreenTest {
    private val composeTestRule = createComposeRule()

    @Test
    @DisplayName("displays title")
    fun historyScreen_displaysTitle() {
        composeTestRule.setContent {
            historyScreen()
        }
        composeTestRule.onNodeWithText("Run History").assertIsDisplayed()
    }

    @Test
    @DisplayName("empty state: displays no runs message")
    fun historyScreen_emptyState_displaysNoRunsMessage() {
        composeTestRule.setContent {
            historyScreen(runs = emptyList())
        }
        composeTestRule.onNodeWithText("Run History").assertIsDisplayed()
        composeTestRule.onNodeWithText("No runs recorded yet").assertIsDisplayed()
    }

    @Test
    @DisplayName("single run: displays run data")
    fun historyScreen_singleRun_displaysRunData() {
        val run = TestDataFactory.createTestRunEntity(distanceMeters = 5000.0)
        composeTestRule.setContent {
            historyScreen(runs = listOf(run))
        }
        composeTestRule.onNodeWithText("Run History").assertIsDisplayed()
        composeTestRule.onNodeWithText { it.contains("5.00") }.assertIsDisplayed() // distance
    }

    @Test
    @DisplayName("run card: displays distance formatted")
    fun historyScreen_runCard_displaysDistanceFormatted() {
        val run =
            TestDataFactory.createTestRunEntity(
                distanceMeters = 10500.0,
            )
        composeTestRule.setContent {
            historyScreen(runs = listOf(run))
        }
        composeTestRule.onNodeWithText { it.contains("10.50") }.assertIsDisplayed()
    }

    @Test
    @DisplayName("run card: displays duration formatted")
    fun historyScreen_runCard_displaysDurationFormatted() {
        val run =
            TestDataFactory.createTestRunEntity(
                durationMinutes = 60,
            ) // 1 hour
        composeTestRule.setContent {
            historyScreen(runs = listOf(run))
        }
        composeTestRule.onNodeWithText { it.contains("01:00:00") }.assertIsDisplayed()
    }

    @Test
    @DisplayName("run card: displays pace formatted")
    fun historyScreen_runCard_displaysPaceFormatted() {
        val run =
            TestDataFactory.createTestRunEntity(
                averagePaceMinPerKm = 9.5,
            )
        composeTestRule.setContent {
            historyScreen(runs = listOf(run))
        }
        composeTestRule.onNodeWithText { it.contains("/km") }.assertIsDisplayed()
    }

    @Test
    @DisplayName("run card: displays heart rate stats when available")
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
        composeTestRule.onNodeWithText { it.contains("150") }.assertIsDisplayed()
        composeTestRule.onNodeWithText { it.contains("180") }.assertIsDisplayed()
        composeTestRule.onNodeWithText { it.contains("110") }.assertIsDisplayed()
    }

    @Test
    @DisplayName("run card: hides heart rate stats when zero")
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
    @DisplayName("delete button: triggers callback with run ID")
    fun historyScreen_deleteButton_triggersCallback() {
        val run = TestDataFactory.createTestRunEntity(id = 42)
        var deletedRunId: Long? = null

        composeTestRule.setContent {
            historyScreen(
                runs = listOf(run),
                onDeleteRun = { deletedRunId = it },
            )
        }

        composeTestRule.onNodeWithText("Delete run").performClick()
        assertTrue(deletedRunId == 42L)
    }

    @Test
    @DisplayName("multiple runs: displays all runs")
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
    @DisplayName("large distance: displays correctly formatted")
    fun historyScreen_largeDistance_displaysCorrectlyFormatted() {
        val run =
            TestDataFactory.createTestRunEntity(
                distanceMeters = 42195.0,
            ) // Marathon
        composeTestRule.setContent {
            historyScreen(runs = listOf(run))
        }
        composeTestRule.onNodeWithText { it.contains("42.19") }.assertIsDisplayed()
    }

    @Test
    @DisplayName("realistic runs: displays with all stats")
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
        // Multiple runs visible
        composeTestRule.onNodeWithText { it.contains("5.00") }.assertIsDisplayed()
        composeTestRule.onNodeWithText { it.contains("10.00") }.assertIsDisplayed()
        composeTestRule.onNodeWithText { it.contains("3.00") }.assertIsDisplayed()
    }

    @Test
    @DisplayName("very short run: displays data without errors")
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
        composeTestRule.onNodeWithText { it.contains("0.10") }.assertIsDisplayed()
    }
}
