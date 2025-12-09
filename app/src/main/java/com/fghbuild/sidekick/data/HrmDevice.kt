// Represents a Bluetooth Heart Rate Monitor device with address, name, and signal strength.
package com.fghbuild.sidekick.data

data class HrmDevice(
    val address: String,
    val name: String,
    val rssi: Int = 0,
)
