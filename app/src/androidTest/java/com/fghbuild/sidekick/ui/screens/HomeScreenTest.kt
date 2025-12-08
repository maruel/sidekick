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
            homeScreen(
                onStartRun = {},
                runData = com.fghbuild.sidekick.data.RunData(),
                heartRateData = com.fghbuild.sidekick.data.HeartRateData(),
                connectedDevice = null,
                userAge = 30,
                discoveredDevices = emptyList(),
                isScanning = false,
                onStartScanning = {},
                onStopScanning = {},
                onSelectDevice = {},
                onDisconnect = {},
                gpsAccuracyMeters = kotlinx.coroutines.flow.MutableStateFlow(5.0f),
                currentLocation = kotlinx.coroutines.flow.MutableStateFlow(null),
            )
        }
        composeTestRule.onNodeWithText("Start Run").assertIsDisplayed()
    }

    @Test
    fun homeScreen_notRunning_showsStartButton() {
        composeTestRule.setContent {
            homeScreen(
                onStartRun = {},
                runData = com.fghbuild.sidekick.data.RunData(),
                heartRateData = com.fghbuild.sidekick.data.HeartRateData(),
                connectedDevice = null,
                userAge = 30,
                discoveredDevices = emptyList(),
                isScanning = false,
                onStartScanning = {},
                onStopScanning = {},
                onSelectDevice = {},
                onDisconnect = {},
                gpsAccuracyMeters = kotlinx.coroutines.flow.MutableStateFlow(5.0f),
                currentLocation = kotlinx.coroutines.flow.MutableStateFlow(null),
            )
        }
        composeTestRule.onNodeWithText("Start Run").assertIsDisplayed()
    }

    @Test
    fun homeScreen_alwaysShowsStartButton() {
        composeTestRule.setContent {
            homeScreen(
                onStartRun = {},
                runData = com.fghbuild.sidekick.data.RunData(),
                heartRateData = com.fghbuild.sidekick.data.HeartRateData(),
                connectedDevice = null,
                userAge = 30,
                discoveredDevices = emptyList(),
                isScanning = false,
                onStartScanning = {},
                onStopScanning = {},
                onSelectDevice = {},
                onDisconnect = {},
                gpsAccuracyMeters = kotlinx.coroutines.flow.MutableStateFlow(5.0f),
                currentLocation = kotlinx.coroutines.flow.MutableStateFlow(null),
            )
        }
        composeTestRule.onNodeWithText("Start Run").assertIsDisplayed()
    }

    @Test
    fun homeScreen_startButtonClick_callsOnStartRun() {
        var startClicked = false
        composeTestRule.setContent {
            homeScreen(
                onStartRun = { startClicked = true },
                runData = com.fghbuild.sidekick.data.RunData(),
                heartRateData = com.fghbuild.sidekick.data.HeartRateData(),
                connectedDevice = null,
                userAge = 30,
                discoveredDevices = emptyList(),
                isScanning = false,
                onStartScanning = {},
                onStopScanning = {},
                onSelectDevice = {},
                onDisconnect = {},
                gpsAccuracyMeters = kotlinx.coroutines.flow.MutableStateFlow(5.0f),
                currentLocation = kotlinx.coroutines.flow.MutableStateFlow(null),
            )
        }
        composeTestRule.onNodeWithText("Start Run").performClick()
        assertTrue(startClicked)
    }
}
