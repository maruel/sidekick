package com.fghbuild.sidekick.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fghbuild.sidekick.data.HeartRateData
import com.fghbuild.sidekick.data.RunData
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
    @DisplayName("displays distance")
    fun runInProgressScreen_displaysDistance() {
        composeTestRule.setContent {
            runInProgressScreen(
                runData = RunData(distanceMeters = 2500.0),
            )
        }
        composeTestRule.onNodeWithText("Distance: 2.50 km").assertIsDisplayed()
    }

    @Test
    @DisplayName("displays pace")
    fun runInProgressScreen_displaysPace() {
        composeTestRule.setContent {
            runInProgressScreen(
                runData = RunData(paceMinPerKm = 5.5),
            )
        }
        composeTestRule.onNodeWithText("Pace: 5:30 min/km").assertIsDisplayed()
    }

    @Test
    @DisplayName("displays heart rate")
    fun runInProgressScreen_displaysHeartRate() {
        composeTestRule.setContent {
            runInProgressScreen(
                heartRateData = HeartRateData(currentBpm = 150),
            )
        }
        composeTestRule.onNodeWithText("Heart Rate: 150 bpm").assertIsDisplayed()
    }

    @Test
    @DisplayName("displays duration")
    fun runInProgressScreen_displaysDuration() {
        composeTestRule.setContent {
            runInProgressScreen(
                runData = RunData(durationMillis = 1800000),
            )
        }
        composeTestRule.onNodeWithText("Duration: 00:30:00").assertIsDisplayed()
    }

    @Test
    @DisplayName("resume button: calls onResume")
    fun runInProgressScreen_resumeButton_callsOnResume() {
        var resumeClicked = false
        composeTestRule.setContent {
            runInProgressScreen(
                onResume = { resumeClicked = true },
            )
        }
        composeTestRule.onNodeWithContentDescription("Resume").performClick()
        assertTrue(resumeClicked)
    }

    @Test
    @DisplayName("stop button: calls onStop")
    fun runInProgressScreen_stopButton_callsOnStop() {
        var stopClicked = false
        composeTestRule.setContent {
            runInProgressScreen(
                onStop = { stopClicked = true },
            )
        }
        composeTestRule.onNodeWithContentDescription("Stop").performClick()
        assertTrue(stopClicked)
    }
}
