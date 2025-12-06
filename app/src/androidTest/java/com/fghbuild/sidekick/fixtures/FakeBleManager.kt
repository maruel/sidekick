package com.fghbuild.sidekick.fixtures

import com.fghbuild.sidekick.data.HeartRateData
import com.fghbuild.sidekick.data.HrmDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake BLE manager for testing.
 * Simulates Bluetooth device scanning, connection, and heart rate data streaming.
 */
class FakeBleManager {
    private val _discoveredDevices = MutableStateFlow<List<HrmDevice>>(emptyList())
    private val _connectedDevice = MutableStateFlow<HrmDevice?>(null)
    private val _heartRateData = MutableStateFlow(HeartRateData())
    private val _isScanning = MutableStateFlow(false)

    val discoveredDevices: StateFlow<List<HrmDevice>> = _discoveredDevices.asStateFlow()
    val connectedDevice: StateFlow<HrmDevice?> = _connectedDevice.asStateFlow()
    val heartRateData: StateFlow<HeartRateData> = _heartRateData.asStateFlow()
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    /**
     * Simulate starting a BLE scan.
     */
    fun startScanning() {
        _isScanning.value = true
        _discoveredDevices.value = emptyList()
    }

    /**
     * Simulate stopping a BLE scan.
     */
    fun stopScanning() {
        _isScanning.value = false
    }

    /**
     * Inject a fake discovered device during scan.
     */
    fun simulateDeviceDiscovered(device: HrmDevice) {
        val currentDevices = _discoveredDevices.value
        val existingIndex = currentDevices.indexOfFirst { it.address == device.address }
        _discoveredDevices.value =
            if (existingIndex >= 0) {
                // Update existing device (e.g., RSSI)
                currentDevices.toMutableList().apply { set(existingIndex, device) }
            } else {
                // Add new device
                currentDevices + device
            }
    }

    /**
     * Simulate connecting to a discovered device.
     */
    fun connectToDevice(device: HrmDevice) {
        _connectedDevice.value = device
    }

    /**
     * Simulate disconnecting from the device.
     */
    fun disconnect() {
        _connectedDevice.value = null
        _heartRateData.value = HeartRateData()
    }

    /**
     * Feed heart rate data as if received from a BLE device.
     */
    fun feedHeartRateData(heartRateData: HeartRateData) {
        _heartRateData.value = heartRateData
    }

    /**
     * Simulate receiving a single heart rate measurement.
     */
    fun feedHeartRateMeasurement(bpm: Int) {
        val currentData = _heartRateData.value
        val newMeasurements = currentData.measurements + bpm
        val averageBpm =
            if (newMeasurements.isNotEmpty()) {
                newMeasurements.average().toInt()
            } else {
                0
            }

        _heartRateData.value =
            HeartRateData(
                currentBpm = bpm,
                averageBpm = averageBpm,
                measurements = newMeasurements,
            )
    }

    /**
     * Reset all state.
     */
    fun reset() {
        _discoveredDevices.value = emptyList()
        _connectedDevice.value = null
        _heartRateData.value = HeartRateData()
        _isScanning.value = false
    }
}
