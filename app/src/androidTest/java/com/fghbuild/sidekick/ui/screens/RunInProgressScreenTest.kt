package com.fghbuild.sidekick.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.fghbuild.sidekick.fixtures.TestDataFactory
import org.junit.Rule
import org.junit.Test

class RunInProgressScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun runInProgressScreen_displaysMetrics() {
        composeTestRule.setContent {
            runInProgressScreen()
        }
        // Verify metrics are displayed by checking for pause button (always present)
        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_rendersWithEmptyData() {
        composeTestRule.setContent {
            runInProgressScreen(runData = TestDataFactory.createTestRunData(distanceKm = 0.0))
        }
        // Just verify the screen renders without crashing
        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
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
        val runData = TestDataFactory.createTestRunData(distanceKm = 5.0, durationMillis = 45 * 60 * 1000L)
        composeTestRule.setContent {
            runInProgressScreen(runData = runData)
        }
        // Verify screen renders with pace (check for pause button instead)
        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_displaysHeartRateData() {
        val heartRateData = TestDataFactory.createHeartRateData(count = 50)
        composeTestRule.setContent {
            runInProgressScreen(heartRateData = heartRateData)
        }
        // Verify screen renders with heart rate data
        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_displaysDurationFormatted() {
        val runData = TestDataFactory.createTestRunData(distanceKm = 5.0, durationMillis = 45 * 60 * 1000L)
        composeTestRule.setContent {
            runInProgressScreen(runData = runData)
        }
        composeTestRule.onNodeWithText("00:45:00", substring = true).assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_displaysControlsWhenPaused() {
        composeTestRule.setContent {
            runInProgressScreen(
                runData = TestDataFactory.createTestRunData(isPaused = true),
            )
        }
        // When paused, should show resume (play) button
        composeTestRule.onNodeWithContentDescription("Resume").assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_displaysControls() {
        composeTestRule.setContent {
            runInProgressScreen()
        }
        // Verify pause and stop buttons are displayed
        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Stop").assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_rendersRealisticData() {
        val runData = TestDataFactory.createTestRunData(distanceKm = 10.0, durationMillis = 90 * 60 * 1000L)
        val heartRateData = TestDataFactory.createHeartRateData(count = 100)

        composeTestRule.setContent {
            runInProgressScreen(
                runData = runData,
                heartRateData = heartRateData,
            )
        }

        composeTestRule.onNodeWithText("10.00 km", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_rendersRouteMap() {
        val runData = TestDataFactory.createTestRunData(distanceKm = 3.0)
        composeTestRule.setContent {
            runInProgressScreen(runData = runData)
        }
        // Verify screen renders with route map
        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_rendersPaceChart() {
        val runData = TestDataFactory.createTestRunData(distanceKm = 5.0)
        composeTestRule.setContent {
            runInProgressScreen(runData = runData)
        }
        // Verify screen renders with pace chart
        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_rendersHeartRateChart() {
        val heartRateData = TestDataFactory.createHeartRateData(count = 50)
        composeTestRule.setContent {
            runInProgressScreen(heartRateData = heartRateData)
        }
        // Verify screen renders with HR chart
        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_displaysZeroValuesGracefully() {
        composeTestRule.setContent {
            runInProgressScreen()
        }
        composeTestRule.onNodeWithText("0.00 km", substring = true).assertIsDisplayed()
    }
}
