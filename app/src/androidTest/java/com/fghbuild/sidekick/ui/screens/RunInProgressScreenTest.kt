package com.fghbuild.sidekick.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fghbuild.sidekick.data.HeartRateData
import com.fghbuild.sidekick.data.RunData
import org.junit.Assert.assertTrue
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
        composeTestRule.onNodeWithText("Run in Progress").assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_displaysDistance() {
        composeTestRule.setContent {
            runInProgressScreen(
                runData = RunData(distanceMeters = 2500.0),
            )
        }
        composeTestRule.onNodeWithText("Distance: 2.50 km").assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_displaysPace() {
        composeTestRule.setContent {
            runInProgressScreen(
                runData = RunData(paceMinPerKm = 5.5),
            )
        }
        composeTestRule.onNodeWithText("Pace: 5:30 min/km").assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_displaysHeartRate() {
        composeTestRule.setContent {
            runInProgressScreen(
                heartRateData = HeartRateData(currentBpm = 150),
            )
        }
        composeTestRule.onNodeWithText("Heart Rate: 150 bpm").assertIsDisplayed()
    }

    @Test
    fun runInProgressScreen_displaysDuration() {
        composeTestRule.setContent {
            runInProgressScreen(
                runData = RunData(durationMillis = 1800000),
            )
        }
        composeTestRule.onNodeWithText("Duration: 00:30:00").assertIsDisplayed()
    }

    @Test
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
