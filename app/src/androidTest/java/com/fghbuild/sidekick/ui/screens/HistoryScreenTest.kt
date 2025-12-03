package com.fghbuild.sidekick.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fghbuild.sidekick.database.RunEntity
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

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
    @DisplayName("empty list: shows no runs message")
    fun historyScreen_emptyList_showsNoRunsMessage() {
        composeTestRule.setContent {
            historyScreen(runs = emptyList())
        }
        composeTestRule.onNodeWithText("No runs recorded yet").assertIsDisplayed()
    }

    @Test
    @DisplayName("with runs: displays distance")
    fun historyScreen_withRuns_displaysDistance() {
        val runs =
            listOf(
                RunEntity(
                    id = 1,
                    startTime = System.currentTimeMillis(),
                    endTime = System.currentTimeMillis(),
                    distanceMeters = 5000.0,
                    durationMillis = 1800000,
                    averagePaceMinPerKm = 6.0,
                ),
            )
        composeTestRule.setContent {
            historyScreen(runs = runs)
        }
        composeTestRule.onNodeWithText("5.00 km").assertIsDisplayed()
    }

    @Test
    @DisplayName("with runs: displays duration")
    fun historyScreen_withRuns_displaysDuration() {
        val runs =
            listOf(
                RunEntity(
                    id = 1,
                    startTime = System.currentTimeMillis(),
                    endTime = System.currentTimeMillis(),
                    distanceMeters = 5000.0,
                    durationMillis = 1800000,
                    averagePaceMinPerKm = 6.0,
                ),
            )
        composeTestRule.setContent {
            historyScreen(runs = runs)
        }
        composeTestRule.onNodeWithText("00:30:00").assertIsDisplayed()
    }

    @Test
    @DisplayName("delete button: calls onDeleteRun")
    fun historyScreen_deleteButton_callsOnDeleteRun() {
        var deletedRunId: Long? = null
        val runs =
            listOf(
                RunEntity(
                    id = 42,
                    startTime = System.currentTimeMillis(),
                    endTime = System.currentTimeMillis(),
                    distanceMeters = 5000.0,
                    durationMillis = 1800000,
                    averagePaceMinPerKm = 6.0,
                ),
            )
        composeTestRule.setContent {
            historyScreen(
                runs = runs,
                onDeleteRun = { deletedRunId = it },
            )
        }
        composeTestRule.onNodeWithContentDescription("Delete run").performClick()
        assertEquals(42L, deletedRunId)
    }
}
