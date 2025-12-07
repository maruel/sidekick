package com.fghbuild.sidekick.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

class HomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_displaysStartButton() {
        composeTestRule.setContent {
            homeScreen()
        }
        composeTestRule.onNodeWithText("Start Run").assertIsDisplayed()
    }

    @Test
    fun homeScreen_notRunning_showsStartButton() {
        composeTestRule.setContent {
            homeScreen(isRunning = false)
        }
        composeTestRule.onNodeWithText("Start Run").assertIsDisplayed()
    }

    @Test
    fun homeScreen_alwaysShowsStartButton() {
        composeTestRule.setContent {
            homeScreen(isRunning = false)
        }
        composeTestRule.onNodeWithText("Start Run").assertIsDisplayed()
    }

    @Test
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
}
