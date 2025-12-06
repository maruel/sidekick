package com.fghbuild.sidekick.data

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@DisplayName("HrmDevice Tests")
class HrmDeviceTest {
    @Test
    fun create_storesCorrectValues() {
        val device =
            HrmDevice(
                address = "AA:BB:CC:DD:EE:FF",
                name = "Heart Rate Monitor",
                rssi = -65,
            )
        assertEquals("AA:BB:CC:DD:EE:FF", device.address)
        assertEquals("Heart Rate Monitor", device.name)
        assertEquals(-65, device.rssi)
    }

    @Test
    fun defaultRssi_isZero() {
        val device =
            HrmDevice(
                address = "AA:BB:CC:DD:EE:FF",
                name = "Heart Rate Monitor",
            )
        assertEquals(0, device.rssi)
    }

    @Test
    fun copy_preservesValues() {
        val original =
            HrmDevice(
                address = "AA:BB:CC:DD:EE:FF",
                name = "Heart Rate Monitor",
                rssi = -65,
            )
        val copy = original.copy()
        assertEquals(original.address, copy.address)
        assertEquals(original.name, copy.name)
        assertEquals(original.rssi, copy.rssi)
    }

    @Test
    fun copy_canModifySingleField() {
        val original =
            HrmDevice(
                address = "AA:BB:CC:DD:EE:FF",
                name = "Heart Rate Monitor",
                rssi = -65,
            )
        val copy = original.copy(rssi = -45)
        assertEquals("AA:BB:CC:DD:EE:FF", copy.address)
        assertEquals(-45, copy.rssi)
    }

    @Test
    fun equality_worksCorrectly() {
        val device1 = HrmDevice("AA:BB:CC:DD:EE:FF", "HRM", -65)
        val device2 = HrmDevice("AA:BB:CC:DD:EE:FF", "HRM", -65)
        assertEquals(device1, device2)
    }
}
