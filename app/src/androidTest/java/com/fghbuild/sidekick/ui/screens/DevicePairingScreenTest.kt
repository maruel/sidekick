package com.fghbuild.sidekick.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fghbuild.sidekick.data.HrmDevice
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

class DevicePairingScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun devicePairingScreen_displaysTitle() {
        composeTestRule.setContent {
            devicePairingScreen()
        }
        composeTestRule.onNodeWithText("Heart Rate Monitor").assertIsDisplayed()
    }

    @Test
    fun devicePairingScreen_notConnected_showsAvailableDevicesText() {
        composeTestRule.setContent {
            devicePairingScreen(connectedDevice = null)
        }
        composeTestRule.onNodeWithText("Available Devices").assertIsDisplayed()
    }

    @Test
    fun devicePairingScreen_notConnected_showsStartScanButton() {
        composeTestRule.setContent {
            devicePairingScreen(connectedDevice = null)
        }
        composeTestRule.onNodeWithText("Start Scanning").assertIsDisplayed()
    }

    @Test
    fun devicePairingScreen_scanning_showsProgressIndicator() {
        composeTestRule.setContent {
            devicePairingScreen(
                connectedDevice = null,
                isScanning = true,
            )
        }
        composeTestRule.onNodeWithText("Scanning for devices...").assertIsDisplayed()
    }

    @Test
    fun devicePairingScreen_scanning_showsStopScanButton() {
        composeTestRule.setContent {
            devicePairingScreen(
                connectedDevice = null,
                isScanning = true,
            )
        }
        composeTestRule.onNodeWithText("Stop Scanning").assertIsDisplayed()
    }

    @Test
    fun devicePairingScreen_noDevices_showsEmptyMessage() {
        composeTestRule.setContent {
            devicePairingScreen(
                connectedDevice = null,
                isScanning = false,
                discoveredDevices = emptyList(),
            )
        }
        composeTestRule.onNodeWithText("No devices found. Tap Start Scanning to begin.").assertIsDisplayed()
    }

    @Test
    fun devicePairingScreen_withDevices_displaysDeviceList() {
        val testDevice =
            HrmDevice(
                address = "AA:BB:CC:DD:EE:FF",
                name = "Test HR Monitor",
                rssi = -50,
            )
        composeTestRule.setContent {
            devicePairingScreen(
                connectedDevice = null,
                isScanning = false,
                discoveredDevices = listOf(testDevice),
            )
        }
        composeTestRule.onNodeWithText("Test HR Monitor").assertIsDisplayed()
        composeTestRule.onNodeWithText("AA:BB:CC:DD:EE:FF").assertIsDisplayed()
    }

    @Test
    fun devicePairingScreen_withDevices_showsConnectButton() {
        val testDevice =
            HrmDevice(
                address = "AA:BB:CC:DD:EE:FF",
                name = "Test HR Monitor",
            )
        composeTestRule.setContent {
            devicePairingScreen(
                connectedDevice = null,
                discoveredDevices = listOf(testDevice),
            )
        }
        composeTestRule.onNodeWithText("Connect").assertIsDisplayed()
    }

    @Test
    fun devicePairingScreen_connected_showsConnectedUI() {
        val testDevice =
            HrmDevice(
                address = "AA:BB:CC:DD:EE:FF",
                name = "Test HR Monitor",
            )
        composeTestRule.setContent {
            devicePairingScreen(connectedDevice = testDevice)
        }
        composeTestRule.onNodeWithText("Connected").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test HR Monitor").assertIsDisplayed()
        composeTestRule.onNodeWithText("Disconnect").assertIsDisplayed()
    }

    @Test
    fun devicePairingScreen_connected_hidesAvailableDevicesSection() {
        val testDevice =
            HrmDevice(
                address = "AA:BB:CC:DD:EE:FF",
                name = "Test HR Monitor",
            )
        composeTestRule.setContent {
            devicePairingScreen(connectedDevice = testDevice)
        }
        composeTestRule.onNodeWithText("Available Devices").assertIsNotDisplayed()
    }

    @Test
    fun devicePairingScreen_startScanButton_callsOnStartScanning() {
        var startScanClicked = false
        composeTestRule.setContent {
            devicePairingScreen(
                connectedDevice = null,
                isScanning = false,
                onStartScanning = { startScanClicked = true },
            )
        }
        composeTestRule.onNodeWithText("Start Scanning").performClick()
        assertTrue(startScanClicked)
    }

    @Test
    fun devicePairingScreen_stopScanButton_callsOnStopScanning() {
        var stopScanClicked = false
        composeTestRule.setContent {
            devicePairingScreen(
                connectedDevice = null,
                isScanning = true,
                onStopScanning = { stopScanClicked = true },
            )
        }
        composeTestRule.onNodeWithText("Stop Scanning").performClick()
        assertTrue(stopScanClicked)
    }

    @Test
    fun devicePairingScreen_connectButton_callsOnSelectDevice() {
        var selectedDevice: HrmDevice? = null
        val testDevice =
            HrmDevice(
                address = "AA:BB:CC:DD:EE:FF",
                name = "Test HR Monitor",
            )
        composeTestRule.setContent {
            devicePairingScreen(
                connectedDevice = null,
                discoveredDevices = listOf(testDevice),
                onSelectDevice = { device -> selectedDevice = device },
            )
        }
        composeTestRule.onNodeWithText("Connect").performClick()
        assertTrue(selectedDevice == testDevice)
    }

    @Test
    fun devicePairingScreen_disconnectButton_callsOnDisconnect() {
        var disconnectClicked = false
        val testDevice =
            HrmDevice(
                address = "AA:BB:CC:DD:EE:FF",
                name = "Test HR Monitor",
            )
        composeTestRule.setContent {
            devicePairingScreen(
                connectedDevice = testDevice,
                onDisconnect = { disconnectClicked = true },
            )
        }
        composeTestRule.onNodeWithText("Disconnect").performClick()
        assertTrue(disconnectClicked)
    }
}
