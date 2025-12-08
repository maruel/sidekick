package com.fghbuild.sidekick.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OnboardingScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun onboardingScreen_displaysWelcomeMessage() {
        composeTestRule.setContent {
            onboardingScreen(
                onBirthYearSubmit = {},
            )
        }
        composeTestRule.onNodeWithText("Welcome to Sidekick").assertIsDisplayed()
    }

    @Test
    fun onboardingScreen_displaysBirthYearField() {
        composeTestRule.setContent {
            onboardingScreen(
                onBirthYearSubmit = {},
            )
        }
        composeTestRule.onNodeWithText("Birth Year").assertIsDisplayed()
    }

    @Test
    fun onboardingScreen_displaysGetStartedButton() {
        composeTestRule.setContent {
            onboardingScreen(
                onBirthYearSubmit = {},
            )
        }
        composeTestRule.onNodeWithText("Get Started").assertIsDisplayed()
    }

    @Test
    fun onboardingScreen_enterBirthYear_submitsCorrectValue() {
        var submittedYear: Int? = null
        composeTestRule.setContent {
            onboardingScreen(
                onBirthYearSubmit = { year -> submittedYear = year },
            )
        }

        composeTestRule.onNodeWithTag("birthYearInput").performTextInput("1990")
        composeTestRule.onNodeWithText("Get Started").performClick()

        assertEquals(1990, submittedYear)
    }

    @Test
    fun onboardingScreen_invalidYear_doesNotSubmit() {
        var submittedYear: Int? = null
        composeTestRule.setContent {
            onboardingScreen(
                onBirthYearSubmit = { year -> submittedYear = year },
            )
        }

        composeTestRule.onNodeWithTag("birthYearInput").performTextInput("1800")
        composeTestRule.onNodeWithText("Get Started").performClick()

        assertNull(submittedYear)
    }

    @Test
    fun onboardingScreen_emptyInput_doesNotSubmit() {
        var submittedYear: Int? = null
        composeTestRule.setContent {
            onboardingScreen(
                onBirthYearSubmit = { year -> submittedYear = year },
            )
        }

        composeTestRule.onNodeWithText("Get Started").performClick()

        assertNull(submittedYear)
    }
}
