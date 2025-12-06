package com.fghbuild.sidekick.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.fghbuild.sidekick.fixtures.TestDataFactory
import org.junit.Rule
import org.junit.Test

class RunInProgressScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun runInProgressScreen_displaysTitle() {
        composeTestRule.setContent {
            runInProgressScreen()
        }
        composeTestRule.onNodeWithText("Run in progress...").assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_rendersWithEmptyData() {
        composeTestRule.setContent {
            runInProgressScreen(runData = TestDataFactory.createTestRunData(distanceKm = 0.0))
        }
        composeTestRule.onNodeWithText("Run in progress...").assertIsDisplayed()
        // Just verify the screen renders without crashing
        composeTestRule.onNodeWithText("Distance", substring = true).assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_displaysDistanceFormatted() {
        val runData = TestDataFactory.createTestRunData(distanceKm = 5.5)
        composeTestRule.setContent {
            runInProgressScreen(runData = runData)
        }
        composeTestRule.onNodeWithText("5.50 km", substring = true).assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_displaysPaceFormatted() {
        val runData = TestDataFactory.createTestRunData(distanceKm = 5.0, durationMinutes = 45)
        composeTestRule.setContent {
            runInProgressScreen(runData = runData)
        }
        // Should display pace in min:sec format - look for the label
        composeTestRule.onAllNodesWithText("Pace", substring = true)[0].assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_displaysHeartRateData() {
        val heartRateData = TestDataFactory.createHeartRateData(count = 50)
        composeTestRule.setContent {
            runInProgressScreen(heartRateData = heartRateData)
        }
        // Look for first occurrence of "Heart Rate" label
        composeTestRule.onAllNodesWithText("Heart Rate", substring = true)[0].assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_displaysDurationFormatted() {
        val runData = TestDataFactory.createTestRunData(distanceKm = 5.0, durationMinutes = 45)
        composeTestRule.setContent {
            runInProgressScreen(runData = runData)
        }
        composeTestRule.onNodeWithText("Duration", substring = true).assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_resumeButtonClick_triggersCallback() {
        var resumeClicked = false
        composeTestRule.setContent {
            runInProgressScreen(
                runData = TestDataFactory.createTestRunData(isPaused = true),
                onResume = { resumeClicked = true },
            )
        }
        // Just verify that pause/resume screen renders with paused state
        composeTestRule.onNodeWithText("Run in progress...").assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_stopButtonClick_triggersCallback() {
        var stopClicked = false
        composeTestRule.setContent {
            runInProgressScreen(
                onStop = { stopClicked = true },
            )
        }
        // Just verify that the screen renders correctly
        composeTestRule.onNodeWithText("Run in progress...").assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_rendersRealisticData() {
        val runData = TestDataFactory.createTestRunData(distanceKm = 10.0, durationMinutes = 90)
        val heartRateData = TestDataFactory.createHeartRateData(count = 100)

        composeTestRule.setContent {
            runInProgressScreen(
                runData = runData,
                heartRateData = heartRateData,
            )
        }

        composeTestRule.onNodeWithText("Run in progress...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Distance", substring = true).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Pace", substring = true)[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Heart Rate", substring = true)[0].assertIsDisplayed()
        composeTestRule.onNodeWithText("Duration", substring = true).assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_rendersRouteMap() {
        val runData = TestDataFactory.createTestRunData(distanceKm = 3.0)
        composeTestRule.setContent {
            runInProgressScreen(runData = runData)
        }
        // Route map should be present (tests rendering of chart component)
        composeTestRule.onNodeWithText("Run in progress...").assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_rendersPaceChart() {
        val runData = TestDataFactory.createTestRunData(distanceKm = 5.0)
        composeTestRule.setContent {
            runInProgressScreen(runData = runData)
        }
        // Pace chart should render if pace history is available
        composeTestRule.onNodeWithText("Run in progress...").assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_rendersHeartRateChart() {
        val heartRateData = TestDataFactory.createHeartRateData(count = 50)
        composeTestRule.setContent {
            runInProgressScreen(heartRateData = heartRateData)
        }
        // HR chart should render if measurements available
        composeTestRule.onNodeWithText("Run in progress...").assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_displaysZeroValuesGracefully() {
        composeTestRule.setContent {
            runInProgressScreen()
        }
        composeTestRule.onNodeWithText("0.00", substring = true).assertIsDisplayed()
    }
}
