package com.fghbuild.sidekick.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@DisplayName("HomeScreen Tests")
class HomeScreenTest {
    private val composeTestRule = createComposeRule()

    @Test
    @DisplayName("displays title")
    fun homeScreen_displaysTitle() {
        composeTestRule.setContent {
            homeScreen()
        }
        composeTestRule.onNodeWithText("Sidekick").assertIsDisplayed()
    }

    @Test
    @DisplayName("not running: shows ready message")
    fun homeScreen_notRunning_showsReadyMessage() {
        composeTestRule.setContent {
            homeScreen(isRunning = false)
        }
        composeTestRule.onNodeWithText("Ready to run!").assertIsDisplayed()
    }

    @Test
    @DisplayName("not running: shows start button")
    fun homeScreen_notRunning_showsStartButton() {
        composeTestRule.setContent {
            homeScreen(isRunning = false)
        }
        composeTestRule.onNodeWithText("Start Run").assertIsDisplayed()
    }

    @Test
    @DisplayName("running: shows in progress message")
    fun homeScreen_running_showsInProgressMessage() {
        composeTestRule.setContent {
            homeScreen(isRunning = true)
        }
        composeTestRule.onNodeWithText("Run in progress...").assertIsDisplayed()
    }

    @Test
    @DisplayName("running: shows stop button")
    fun homeScreen_running_showsStopButton() {
        composeTestRule.setContent {
            homeScreen(isRunning = true)
        }
        composeTestRule.onNodeWithText("Stop Run").assertIsDisplayed()
    }

    @Test
    @DisplayName("start button click: calls onStartRun")
    fun homeScreen_startButtonClick_callsOnStartRun() {
        var startClicked = false
        composeTestRule.setContent {
            homeScreen(
                isRunning = false,
                onStartRun = { startClicked = true },
            )
        }
        composeTestRule.onNodeWithText("Start Run").performClick()
        assertTrue(startClicked)
    }

    @Test
    @DisplayName("stop button click: calls onStopRun")
    fun homeScreen_stopButtonClick_callsOnStopRun() {
        var stopClicked = false
        composeTestRule.setContent {
            homeScreen(
                isRunning = true,
                onStopRun = { stopClicked = true },
            )
        }
        composeTestRule.onNodeWithText("Stop Run").performClick()
        assertTrue(stopClicked)
    }
}
