package com.fghbuild.sidekick.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fghbuild.sidekick.database.RunEntity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun historyScreen_displaysTitle() {
        composeTestRule.setContent {
            historyScreen()
        }
        composeTestRule.onNodeWithText("Run History").assertIsDisplayed()
    }

    @Test
    fun historyScreen_emptyList_showsNoRunsMessage() {
        composeTestRule.setContent {
            historyScreen(runs = emptyList())
        }
        composeTestRule.onNodeWithText("No runs recorded yet").assertIsDisplayed()
    }

    @Test
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
