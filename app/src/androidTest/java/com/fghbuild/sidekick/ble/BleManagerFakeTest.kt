package com.fghbuild.sidekick.ble

import com.fghbuild.sidekick.data.HrmDevice
import com.fghbuild.sidekick.fixtures.FakeBleManager
import com.fghbuild.sidekick.fixtures.TestDataFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BleManagerFakeTest {
    private lateinit var bleManager: FakeBleManager

    @Before
    fun setup() {
        bleManager = FakeBleManager()
    }

    @Test
    fun scan_discoversFakeDevices() =
        runBlocking {
            bleManager.startScanning()

            val device1 = HrmDevice(address = "AA:BB:CC:DD:EE:F1", name = "Garmin HRM", rssi = -50)
            val device2 = HrmDevice(address = "AA:BB:CC:DD:EE:F2", name = "Polar HRM", rssi = -60)

            bleManager.simulateDeviceDiscovered(device1)
            bleManager.simulateDeviceDiscovered(device2)

            val discoveredDevices = bleManager.discoveredDevices.first()
            assertEquals(2, discoveredDevices.size)
            assertTrue(discoveredDevices.any { it.name == "Garmin HRM" })
            assertTrue(discoveredDevices.any { it.name == "Polar HRM" })
        }

    @Test
    fun connect_transitionsToConnected() =
        runBlocking {
            val device = HrmDevice(address = "AA:BB:CC:DD:EE:F1", name = "Garmin HRM", rssi = -50)

            bleManager.connectToDevice(device)

            val connectedDevice = bleManager.connectedDevice.first()
            assertNotNull(connectedDevice)
            assertEquals(device.address, connectedDevice.address)
        }

    @Test
    fun heartRate_receivesSingleMeasurement() =
        runBlocking {
            val device = HrmDevice(address = "AA:BB:CC:DD:EE:F1", name = "Garmin HRM", rssi = -50)
            bleManager.connectToDevice(device)

            bleManager.feedHeartRateMeasurement(150)

            val heartRateData = bleManager.heartRateData.first()
            assertEquals(150, heartRateData.currentBpm)
            assertEquals(1, heartRateData.measurements.size)
        }

    @Test
    fun heartRate_streamsMultipleMeasurements() =
        runBlocking {
            val device = HrmDevice(address = "AA:BB:CC:DD:EE:F1", name = "Garmin HRM", rssi = -50)
            bleManager.connectToDevice(device)

            val heartRateData = TestDataFactory.createHeartRateData(count = 10)
            bleManager.feedHeartRateData(heartRateData)

            val receivedData = bleManager.heartRateData.first()
            assertEquals(10, receivedData.measurements.size)
            assertTrue(receivedData.averageBpm > 0)
        }

    @Test
    fun heartRate_calculatesAverageFromStream() =
        runBlocking {
            val device = HrmDevice(address = "AA:BB:CC:DD:EE:F1", name = "Garmin HRM", rssi = -50)
            bleManager.connectToDevice(device)

            val measurements = listOf(100, 120, 140, 160, 180)
            for (bpm in measurements) {
                bleManager.feedHeartRateMeasurement(bpm)
            }

            val heartRateData = bleManager.heartRateData.first()
            val expectedAverage = measurements.average().toInt()
            assertEquals(expectedAverage, heartRateData.averageBpm)
        }

    @Test
    fun heartRate_tracksMinMaxDuringRun() =
        runBlocking {
            val device = HrmDevice(address = "AA:BB:CC:DD:EE:F1", name = "Garmin HRM", rssi = -50)
            bleManager.connectToDevice(device)

            val measurements = listOf(100, 120, 140, 160, 180, 170, 150, 130, 120, 110)
            for (bpm in measurements) {
                bleManager.feedHeartRateMeasurement(bpm)
            }

            val heartRateData = bleManager.heartRateData.first()
            assertEquals(100, heartRateData.measurements.minOrNull())
            assertEquals(180, heartRateData.measurements.maxOrNull())
        }

    @Test
    fun disconnect_clearsHeartRateData() =
        runBlocking {
            val device = HrmDevice(address = "AA:BB:CC:DD:EE:F1", name = "Garmin HRM", rssi = -50)
            bleManager.connectToDevice(device)

            bleManager.feedHeartRateMeasurement(150)
            var heartRateData = bleManager.heartRateData.first()
            assertEquals(150, heartRateData.currentBpm)

            bleManager.disconnect()

            heartRateData = bleManager.heartRateData.first()
            assertEquals(0, heartRateData.currentBpm)
            assertEquals(0, heartRateData.measurements.size)
            assertEquals(null, bleManager.connectedDevice.first())
        }

    @Test
    fun scan_duplicateDevicesUpdateRssi() =
        runBlocking {
            bleManager.startScanning()

            val device1 = HrmDevice(address = "AA:BB:CC:DD:EE:F1", name = "Garmin HRM", rssi = -50)
            bleManager.simulateDeviceDiscovered(device1)

            val discoveredBefore = bleManager.discoveredDevices.first()
            assertEquals(1, discoveredBefore.size)
            assertEquals(-50, discoveredBefore[0].rssi)

            // Simulate rediscovery with different RSSI
            val device1Updated =
                HrmDevice(address = "AA:BB:CC:DD:EE:F1", name = "Garmin HRM", rssi = -40)
            bleManager.simulateDeviceDiscovered(device1Updated)

            val discoveredAfter = bleManager.discoveredDevices.first()
            assertEquals(1, discoveredAfter.size)
            assertEquals(-40, discoveredAfter[0].rssi)
        }

    @Test
    fun reset_clearsAllState() =
        runBlocking {
            val device = HrmDevice(address = "AA:BB:CC:DD:EE:F1", name = "Garmin HRM", rssi = -50)
            bleManager.simulateDeviceDiscovered(device)
            bleManager.connectToDevice(device)
            bleManager.feedHeartRateMeasurement(150)

            bleManager.reset()

            assertEquals(0, bleManager.discoveredDevices.first().size)
            assertEquals(null, bleManager.connectedDevice.first())
            assertEquals(0, bleManager.heartRateData.first().currentBpm)
            assertEquals(false, bleManager.isScanning.first())
        }

    @Test
    fun realisticRun_deviceDiscoveryAndConnectionFlow() =
        runBlocking {
            // Start scan
            bleManager.startScanning()

            // Discover device
            val device = HrmDevice(address = "AA:BB:CC:DD:EE:F1", name = "Garmin HRM", rssi = -50)
            bleManager.simulateDeviceDiscovered(device)

            var discovered = bleManager.discoveredDevices.first()
            assertEquals(1, discovered.size)

            // Connect
            bleManager.connectToDevice(device)
            var connectedDevice = bleManager.connectedDevice.first()
            assertNotNull(connectedDevice)

            // Stream heart rate
            val heartRateData = TestDataFactory.createHeartRateData(count = 30)
            bleManager.feedHeartRateData(heartRateData)

            var hrData = bleManager.heartRateData.first()
            assertEquals(30, hrData.measurements.size)
            assertTrue(hrData.averageBpm in 100..200)

            // Disconnect
            bleManager.disconnect()
            connectedDevice = bleManager.connectedDevice.first()
            assertEquals(null, connectedDevice)
        }
}
