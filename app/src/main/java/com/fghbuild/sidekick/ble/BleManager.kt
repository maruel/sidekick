package com.fghbuild.sidekick.ble

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import androidx.core.app.ActivityCompat
import com.fghbuild.sidekick.data.HeartRateData
import com.fghbuild.sidekick.data.HrmDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class BleManager(private val context: Context) {
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

    private val _discoveredDevices = MutableStateFlow<List<HrmDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<HrmDevice>> = _discoveredDevices.asStateFlow()

    private val _heartRateData = MutableStateFlow(HeartRateData())
    val heartRateData: StateFlow<HeartRateData> = _heartRateData.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connectedDevice = MutableStateFlow<HrmDevice?>(null)
    val connectedDevice: StateFlow<HrmDevice?> = _connectedDevice.asStateFlow()

    private var bluetoothGatt: BluetoothGatt? = null
    private var scanCallback: ScanCallback? = null

    companion object {
        private val HEART_RATE_SERVICE_UUID =
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        private val HEART_RATE_MEASUREMENT_UUID =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        private val CLIENT_CHARACTERISTIC_CONFIG_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    fun startScanning() {
        if (_isScanning.value || bluetoothLeScanner == null) return

        try {
            if (
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN,
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            _isScanning.value = true
            _discoveredDevices.value = emptyList()

            val filters =
                listOf(
                    ScanFilter.Builder()
                        .setServiceUuid(
                            android.os.ParcelUuid(HEART_RATE_SERVICE_UUID),
                        )
                        .build(),
                )

            val settings =
                ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .build()

            scanCallback =
                object : ScanCallback() {
                    override fun onScanResult(
                        callbackType: Int,
                        result: android.bluetooth.le.ScanResult,
                    ) {
                        super.onScanResult(callbackType, result)
                        val device = result.device
                        val name =
                            if (
                                ActivityCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                device.name ?: "Unknown"
                            } else {
                                "Unknown"
                            }

                        val newDevice =
                            HrmDevice(
                                address = device.address,
                                name = name,
                                rssi = result.rssi,
                            )

                        val currentDevices = _discoveredDevices.value
                        val existingIndex =
                            currentDevices.indexOfFirst { it.address == device.address }
                        _discoveredDevices.value =
                            if (existingIndex >= 0) {
                                currentDevices
                                    .toMutableList()
                                    .apply {
                                        set(existingIndex, newDevice)
                                    }
                            } else {
                                currentDevices + newDevice
                            }
                    }
                }

            bluetoothLeScanner?.startScan(filters, settings, scanCallback!!)
        } catch (e: Exception) {
            e.printStackTrace()
            _isScanning.value = false
        }
    }

    fun stopScanning() {
        if (!_isScanning.value) return

        try {
            if (
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                scanCallback?.let { bluetoothLeScanner?.stopScan(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _isScanning.value = false
    }

    fun connectToDevice(device: HrmDevice) {
        try {
            if (
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT,
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val bluetoothDevice =
                bluetoothAdapter?.getRemoteDevice(device.address)
            bluetoothDevice?.connectGatt(context, false, gattCallback)
            _connectedDevice.value = device
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            if (
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                bluetoothGatt?.disconnect()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val gattCallback =
        object : BluetoothGattCallback() {
            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int,
            ) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    bluetoothGatt = gatt
                    try {
                        if (
                            ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.BLUETOOTH_CONNECT,
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            gatt.discoverServices()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    bluetoothGatt = null
                    _connectedDevice.value = null
                }
            }

            override fun onServicesDiscovered(
                gatt: BluetoothGatt,
                status: Int,
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt.getService(HEART_RATE_SERVICE_UUID)
                    val characteristic =
                        service?.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)
                    characteristic?.let {
                        try {
                            if (
                                ActivityCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                gatt.setCharacteristicNotification(it, true)

                                // Write to CCCD (Client Characteristic Configuration Descriptor)
                                val descriptor =
                                    it.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                                descriptor?.let { desc ->
                                    desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                    gatt.writeDescriptor(desc)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
            ) {
                if (characteristic.uuid == HEART_RATE_MEASUREMENT_UUID) {
                    val bpm = parseHeartRate(value)
                    updateHeartRateData(bpm)
                }
            }
        }

    private fun parseHeartRate(value: ByteArray): Int {
        if (value.isEmpty()) return 0
        val flags = value[0].toInt()
        return if (flags and 0x01 == 0) {
            value[1].toInt() and 0xFF
        } else {
            ((value[2].toInt() and 0xFF) shl 8) or (value[1].toInt() and 0xFF)
        }
    }

    private fun updateHeartRateData(bpm: Int) {
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
}
