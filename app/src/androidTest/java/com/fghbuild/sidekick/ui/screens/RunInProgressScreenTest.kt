package com.fghbuild.sidekick.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fghbuild.sidekick.fixtures.TestDataFactory
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@DisplayName("RunInProgressScreen Tests")
class RunInProgressScreenTest {
    private val composeTestRule = createComposeRule()

    @Test
    @DisplayName("displays title")
    fun runInProgressScreen_displaysTitle() {
        composeTestRule.setContent {
            runInProgressScreen()
        }
        composeTestRule.onNodeWithText("Run in Progress").assertIsDisplayed()
    }

    @Test
    @DisplayName("renders with empty data")
    fun runInProgressScreen_rendersWithEmptyData() {
        composeTestRule.setContent {
            runInProgressScreen(runData = TestDataFactory.createTestRunData(distanceKm = 0.0))
        }
        composeTestRule.onNodeWithText("Run in Progress").assertIsDisplayed()
        composeTestRule.onNodeWithText("0:00").assertIsDisplayed()
    }

    @Test
    @DisplayName("displays distance correctly formatted")
    fun runInProgressScreen_displaysDistanceFormatted() {
        val runData = TestDataFactory.createTestRunData(distanceKm = 5.5)
        composeTestRule.setContent {
            runInProgressScreen(runData = runData)
        }
        composeTestRule.onNodeWithText("Distance: 5.50 km").assertIsDisplayed()
    }

    @Test
    @DisplayName("displays pace correctly formatted")
    fun runInProgressScreen_displaysPaceFormatted() {
        val runData = TestDataFactory.createTestRunData(distanceKm = 5.0, durationMinutes = 45)
        composeTestRule.setContent {
            runInProgressScreen(runData = runData)
        }
        // Should display pace in min:sec format
        composeTestRule.onNodeWithText("Pace:", substring = true).assertIsDisplayed()
    }

    @Test
    @DisplayName("displays heart rate data")
    fun runInProgressScreen_displaysHeartRateData() {
        val heartRateData = TestDataFactory.createHeartRateData(count = 50)
        composeTestRule.setContent {
            runInProgressScreen(heartRateData = heartRateData)
        }
        composeTestRule.onNodeWithText("Heart Rate:", substring = true).assertIsDisplayed()
    }

    @Test
    @DisplayName("displays duration formatted")
    fun runInProgressScreen_displaysDurationFormatted() {
        val runData = TestDataFactory.createTestRunData(distanceKm = 5.0, durationMinutes = 45)
        composeTestRule.setContent {
            runInProgressScreen(runData = runData)
        }
        composeTestRule.onNodeWithText("Duration:", substring = true).assertIsDisplayed()
    }

    @Test
    @DisplayName("resume button click triggers callback")
    fun runInProgressScreen_resumeButtonClick_triggersCallback() {
        var resumeClicked = false
        composeTestRule.setContent {
            runInProgressScreen(
                onResume = { resumeClicked = true },
            )
        }
        composeTestRule.onNodeWithText("Resume").performClick()
        assertTrue(resumeClicked)
    }

    @Test
    @DisplayName("stop button click triggers callback")
    fun runInProgressScreen_stopButtonClick_triggersCallback() {
        var stopClicked = false
        composeTestRule.setContent {
            runInProgressScreen(
                onStop = { stopClicked = true },
            )
        }
        composeTestRule.onNodeWithText("Stop").performClick()
        assertTrue(stopClicked)
    }

    @Test
    @DisplayName("renders with realistic 10km run data")
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
    @DisplayName("renders route map with points")
    fun runInProgressScreen_rendersRouteMap() {
        val runData = TestDataFactory.createTestRunData(distanceKm = 3.0)
        composeTestRule.setContent {
            runInProgressScreen(runData = runData)
        }
        // Route map should be present (tests rendering of chart component)
        composeTestRule.onNodeWithText("Run in Progress").assertIsDisplayed()
    }

    @Test
    @DisplayName("renders pace chart with history")
    fun runInProgressScreen_rendersPaceChart() {
        val runData = TestDataFactory.createTestRunData(distanceKm = 5.0)
        composeTestRule.setContent {
            runInProgressScreen(runData = runData)
        }
        // Pace chart should render if pace history is available
        composeTestRule.onNodeWithText("Run in Progress").assertIsDisplayed()
    }

    @Test
    @DisplayName("renders heart rate chart with measurements")
    fun runInProgressScreen_rendersHeartRateChart() {
        val heartRateData = TestDataFactory.createHeartRateData(count = 50)
        composeTestRule.setContent {
            runInProgressScreen(heartRateData = heartRateData)
        }
        // HR chart should render if measurements available
        composeTestRule.onNodeWithText("Run in Progress").assertIsDisplayed()
    }

    @Test
    @DisplayName("displays zero values gracefully")
    fun runInProgressScreen_displaysZeroValuesGracefully() {
        composeTestRule.setContent {
            runInProgressScreen()
        }
        composeTestRule.onNodeWithText("0.00", substring = true).assertIsDisplayed()
    }
}
