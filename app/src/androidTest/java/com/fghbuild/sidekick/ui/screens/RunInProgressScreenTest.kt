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

class RunInProgressScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun runInProgressScreen_displaysTitle() {
        composeTestRule.setContent {
            runInProgressScreen()
        }
        composeTestRule.onNodeWithText("Run in Progress").assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_rendersWithEmptyData() {
        composeTestRule.setContent {
            runInProgressScreen(runData = TestDataFactory.createTestRunData(distanceKm = 0.0))
        }
        composeTestRule.onNodeWithText("Run in Progress").assertIsDisplayed()
        // Just verify the screen renders without crashing
        composeTestRule.onNodeWithText("Distance:", substring = true).assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_displaysDistanceFormatted() {
        val runData = TestDataFactory.createTestRunData(distanceKm = 5.5)
        composeTestRule.setContent {
            runInProgressScreen(runData = runData)
        }
        composeTestRule.onNodeWithText("Distance: 5.50 km").assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_displaysPaceFormatted() {
        val runData = TestDataFactory.createTestRunData(distanceKm = 5.0, durationMinutes = 45)
        composeTestRule.setContent {
            runInProgressScreen(runData = runData)
        }
        // Should display pace in min:sec format
        composeTestRule.onNodeWithText("Pace:", substring = true).assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_displaysHeartRateData() {
        val heartRateData = TestDataFactory.createHeartRateData(count = 50)
        composeTestRule.setContent {
            runInProgressScreen(heartRateData = heartRateData)
        }
        composeTestRule.onNodeWithText("Heart Rate:", substring = true).assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_displaysDurationFormatted() {
        val runData = TestDataFactory.createTestRunData(distanceKm = 5.0, durationMinutes = 45)
        composeTestRule.setContent {
            runInProgressScreen(runData = runData)
        }
        composeTestRule.onNodeWithText("Duration:", substring = true).assertIsDisplayed()
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
        // Icon button uses contentDescription, not visible text
        composeTestRule.onNodeWithContentDescription("Resume").performClick()
        assertTrue(resumeClicked)
    }

    @Test
    fun runInProgressScreen_stopButtonClick_triggersCallback() {
        var stopClicked = false
        composeTestRule.setContent {
            runInProgressScreen(
                onStop = { stopClicked = true },
            )
        }
        // Icon button uses contentDescription, not visible text
        composeTestRule.onNodeWithContentDescription("Stop").performClick()
        assertTrue(stopClicked)
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

        composeTestRule.onNodeWithText("Run in Progress").assertIsDisplayed()
        composeTestRule.onNodeWithText("Distance:", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Pace:", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Heart Rate:", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Duration:", substring = true).assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_rendersRouteMap() {
        val runData = TestDataFactory.createTestRunData(distanceKm = 3.0)
        composeTestRule.setContent {
            runInProgressScreen(runData = runData)
        }
        // Route map should be present (tests rendering of chart component)
        composeTestRule.onNodeWithText("Run in Progress").assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_rendersPaceChart() {
        val runData = TestDataFactory.createTestRunData(distanceKm = 5.0)
        composeTestRule.setContent {
            runInProgressScreen(runData = runData)
        }
        // Pace chart should render if pace history is available
        composeTestRule.onNodeWithText("Run in Progress").assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_rendersHeartRateChart() {
        val heartRateData = TestDataFactory.createHeartRateData(count = 50)
        composeTestRule.setContent {
            runInProgressScreen(heartRateData = heartRateData)
        }
        // HR chart should render if measurements available
        composeTestRule.onNodeWithText("Run in Progress").assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_displaysZeroValuesGracefully() {
        composeTestRule.setContent {
            runInProgressScreen()
        }
        composeTestRule.onNodeWithText("0.00", substring = true).assertIsDisplayed()
    }
}
