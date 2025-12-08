package com.fghbuild.sidekick.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

class DevicePairingDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun devicePairingDialog_displaysTitle() {
        composeTestRule.setContent {
            devicePairingDialog(
                discoveredDevices = emptyList(),
                connectedDevice = null,
                isScanning = false,
                onStartScanning = {},
                onStopScanning = {},
                onSelectDevice = {},
                onDisconnect = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithText("Heart Rate Monitor").assertIsDisplayed()
    }

    @Test
    fun devicePairingDialog_startsScanning_onOpen() {
        var startScanCalled = false
        composeTestRule.setContent {
            devicePairingDialog(
                discoveredDevices = emptyList(),
                connectedDevice = null,
                isScanning = false,
                onStartScanning = { startScanCalled = true },
                onStopScanning = {},
                onSelectDevice = {},
                onDisconnect = {},
                onDismiss = {},
            )
        }
        assertTrue(startScanCalled)
    }

    @Test
    fun devicePairingDialog_noStartScanButton() {
        composeTestRule.setContent {
            devicePairingDialog(
                discoveredDevices = emptyList(),
                connectedDevice = null,
                isScanning = false,
                onStartScanning = {},
                onStopScanning = {},
                onSelectDevice = {},
                onDisconnect = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithText("Start Scanning").assertIsNotDisplayed()
    }

    @Test
    fun devicePairingDialog_noStopScanButton() {
        composeTestRule.setContent {
            devicePairingDialog(
                discoveredDevices = emptyList(),
                connectedDevice = null,
                isScanning = true,
                onStartScanning = {},
                onStopScanning = {},
                onSelectDevice = {},
                onDisconnect = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithText("Stop Scanning").assertIsNotDisplayed()
    }

    @Test
    fun devicePairingDialog_stopsScanning_onDismiss() {
        var stopScanCalled = false
        composeTestRule.setContent {
            devicePairingDialog(
                discoveredDevices = emptyList(),
                connectedDevice = null,
                isScanning = false,
                onStartScanning = {},
                onStopScanning = { stopScanCalled = true },
                onSelectDevice = {},
                onDisconnect = {},
                onDismiss = {},
            )
        }
        composeTestRule.onNodeWithText("Heart Rate Monitor").assertIsDisplayed()
        // Dismiss the dialog by clicking the close button
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        assertTrue(stopScanCalled)
    }
}
