package com.fghbuild.sidekick.preferences

import android.content.Context
import android.content.SharedPreferences

class DevicePreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("sidekick_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val LAST_HRM_DEVICE_ADDRESS = "last_hrm_device_address"
        private const val LAST_HRM_DEVICE_NAME = "last_hrm_device_name"
    }

    fun saveLastHrmDevice(
        address: String,
        name: String,
    ) {
        sharedPreferences.edit().apply {
            putString(LAST_HRM_DEVICE_ADDRESS, address)
            putString(LAST_HRM_DEVICE_NAME, name)
            apply()
        }
    }

    fun getLastHrmDeviceAddress(): String? {
        return sharedPreferences.getString(LAST_HRM_DEVICE_ADDRESS, null)
    }

    fun getLastHrmDeviceName(): String? {
        return sharedPreferences.getString(LAST_HRM_DEVICE_NAME, null)
    }

    fun clearLastHrmDevice() {
        sharedPreferences.edit().apply {
            remove(LAST_HRM_DEVICE_ADDRESS)
            remove(LAST_HRM_DEVICE_NAME)
            apply()
        }
    }
}
