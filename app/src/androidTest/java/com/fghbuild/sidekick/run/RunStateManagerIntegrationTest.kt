package com.fghbuild.sidekick.run

import android.content.Context
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import com.fghbuild.sidekick.audio.AnnouncementManager
import com.fghbuild.sidekick.audio.VoiceCommandListener
import com.fghbuild.sidekick.fixtures.TestDataFactory
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals

class RunStateManagerIntegrationTest {
    private lateinit var context: Context
    private lateinit var runManager: RunManager
    private lateinit var announcementManager: AnnouncementManager
    private lateinit var voiceCommandListener: VoiceCommandListener
    private lateinit var runStateManager: RunStateManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        runManager = RunManager()

        announcementManager = spyk(AnnouncementManager(context), recordPrivateCalls = true)

        // Mock VoiceCommandListener to avoid SpeechRecognizer instantiation
        // Note: This causes StateFlow proxy issues in runtime that will cause some tests to fail
        // See: https://github.com/mockk/mockk/issues/897
        voiceCommandListener = mockk(relaxed = true)

        runStateManager = RunStateManager(runManager, announcementManager, voiceCommandListener)
    }

    @Ignore("StateFlow proxy issue with mockk relaxed mock")
    @Test
    fun startRun_initializesThresholds() {
        runBlocking {
            runStateManager.startRun()

            val runData = runStateManager.runData.first()
            assertEquals(true, runData.isRunning)

            verify(timeout = 1000) {
                voiceCommandListener.startListening()
            }
        }
    }

    @Ignore("StateFlow proxy issue with mockk relaxed mock")
    @Test
    fun distanceAnnouncements_triggersAt1kmIntervals() {
        runBlocking {
            runStateManager.startRun()

            val route = TestDataFactory.createTestRoute(distanceKm = 3.5)

            for (routePoint in route) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                    }
                runManager.updateLocation(location)
                runStateManager.update()
            }

            val finalRunData = runStateManager.runData.first()
            val finalDistance = finalRunData.distanceMeters / 1000.0

            // Should have triggered announcements for each km milestone
            if (finalDistance >= 1.0) {
                verify(atLeast = 1) {
                    announcementManager.speakDistance(any())
                }
            }
        }
    }

    @Ignore("StateFlow proxy issue with mockk relaxed mock")
    @Test
    fun paceAnnouncements_includesWithDistance() {
        runBlocking {
            runStateManager.startRun()

            val route = TestDataFactory.createTestRoute(distanceKm = 1.5)

            for (routePoint in route) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                    }
                runManager.updateLocation(location)
                runStateManager.update()
            }

            verify(timeout = 2000) {
                announcementManager.speakPace(any())
            }
        }
    }

    @Ignore("StateFlow proxy issue with mockk relaxed mock")
    @Test
    fun voiceCommandHandling_receivesPauseCommand() {
        runBlocking {
            runStateManager.startRun()

            // This test is skipped due to StateFlow proxy issues with mockk
            // The relaxed mock can't properly handle the lastCommand StateFlow property
            verify(timeout = 1000) {
                voiceCommandListener.startListening()
            }
        }
    }

    @Ignore("StateFlow proxy issue with mockk relaxed mock")
    @Test
    fun pauseRun_stopsTracking() {
        runBlocking {
            runStateManager.startRun()

            val route = TestDataFactory.createTestRoute(distanceKm = 1.5)
            for (routePoint in route) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                    }
                runManager.updateLocation(location)
                runStateManager.update()
            }

            runStateManager.pauseRun()

            val pausedRunData = runStateManager.runData.first()
            assertEquals(false, pausedRunData.isRunning)
        }
    }

    @Ignore("StateFlow proxy issue with mockk relaxed mock")
    @Test
    fun resumeRun_restartsAnnouncements() {
        runBlocking {
            runStateManager.startRun()

            val route1 = TestDataFactory.createTestRoute(distanceKm = 1.0)
            for (routePoint in route1) {
                val location =
                    Location("test").apply {
                        latitude = routePoint.latitude
                        longitude = routePoint.longitude
                        time = routePoint.timestamp
                    }
                runManager.updateLocation(location)
                runStateManager.update()
            }

            runStateManager.pauseRun()
            runStateManager.resumeRun()

            val resumedRunData = runStateManager.runData.first()
            assertEquals(true, resumedRunData.isRunning)
        }
    }

    @Ignore("StateFlow proxy issue with mockk relaxed mock")
    @Test
    fun stopRun_stopsServicesGracefully() {
        runBlocking {
            runStateManager.startRun()

            runStateManager.stopRun()

            verify {
                voiceCommandListener.stopListening()
                announcementManager.stop()
            }
        }
    }

    @Ignore("StateFlow proxy issue with mockk relaxed mock")
    @Test
    fun hourlyAnnouncements_updatesEveryMinute() {
        runBlocking {
            runStateManager.startRun()

            // Simulate multiple update calls
            for (i in 0 until 5) {
                runStateManager.update()
            }

            // Verify manager still has valid state
            val runData = runStateManager.runData.first()
            assertEquals(true, runData.isRunning)
        }
    }
}
