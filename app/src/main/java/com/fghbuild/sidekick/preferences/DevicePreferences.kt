// SharedPreferences wrapper for device-specific settings (HRM device, birth year, onboarding state).
package com.fghbuild.sidekick.preferences

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

class DevicePreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("sidekick_prefs", Context.MODE_PRIVATE)
            ?: error("Unable to initialize SharedPreferences for Sidekick")

    companion object {
        private const val LAST_HRM_DEVICE_ADDRESS = "last_hrm_device_address"
        private const val LAST_HRM_DEVICE_NAME = "last_hrm_device_name"
        private const val USER_BIRTH_YEAR = "user_birth_year"
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

    fun saveBirthYear(birthYear: Int) {
        sharedPreferences.edit().apply {
            putInt(USER_BIRTH_YEAR, birthYear)
            apply()
        }
    }

    fun getBirthYear(): Int? {
        val birthYear = sharedPreferences.getInt(USER_BIRTH_YEAR, -1)
        return if (birthYear == -1) null else birthYear
    }

    fun isOnboardingComplete(): Boolean {
        return try {
            // Check if all required onboarding values are set
            // Currently requires birth year; extend this method when adding new questions
            getBirthYear() != null
        } catch (_: RuntimeException) {
            // If SharedPreferences access fails, consider onboarding incomplete
            // This prevents crashes if SharedPreferences is corrupted or unavailable
            false
        }
    }

    fun getCurrentAge(): Int {
        val birthYear = getBirthYear() ?: return 30
        return Calendar.getInstance().get(Calendar.YEAR) - birthYear
    }
}
