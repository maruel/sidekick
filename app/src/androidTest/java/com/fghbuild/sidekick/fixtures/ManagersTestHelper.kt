package com.fghbuild.sidekick.fixtures

/**
 * Helper for creating fake managers in tests.
 * Centralizes manager instantiation and cleanup.
 */
class ManagersTestHelper {
    private val fakeLocationTracker = FakeLocationTracker()
    private val fakeBleManager = FakeBleManager()

    val locationTracker: FakeLocationTracker get() = fakeLocationTracker
    val bleManager: FakeBleManager get() = fakeBleManager

    /**
     * Reset all managers to initial state.
     */
    fun reset() {
        fakeLocationTracker.reset()
        fakeBleManager.reset()
    }

    /**
     * Clean up resources.
     */
    fun cleanup() {
        fakeLocationTracker.stopTracking()
        fakeBleManager.disconnect()
    }
}
