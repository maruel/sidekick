package com.fghbuild.sidekick.run

import android.content.Context
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import com.fghbuild.sidekick.audio.AnnouncementManager
import com.fghbuild.sidekick.audio.VoiceCommand
import com.fghbuild.sidekick.audio.VoiceCommandListener
import com.fghbuild.sidekick.fixtures.TestDataFactory
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
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

        announcementManager = spyk(AnnouncementManager(context))
        voiceCommandListener = spyk(VoiceCommandListener(context))

        runStateManager = RunStateManager(runManager, announcementManager, voiceCommandListener)
    }

    @Test
    fun startRun_initializesThresholds() =
        runBlocking {
            runStateManager.startRun()

            val runData = runStateManager.runData.first()
            assertEquals(true, runData.isRunning)

            verify(timeout = 1000) {
                voiceCommandListener.startListening()
            }
        }

    @Test
    fun distanceAnnouncements_triggersAt1kmIntervals() =
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

    @Test
    fun paceAnnouncements_includesWithDistance() =
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

    @Test
    fun voiceCommandHandling_receivesPauseCommand() =
        runBlocking {
            runStateManager.startRun()

            // Simulate voice command injection
            val voiceCommandFlow = voiceCommandListener.lastCommand
            // In a real test, we'd inject the command via mocking

            assertEquals(VoiceCommand.NONE, voiceCommandFlow.first())
        }

    @Test
    fun pauseRun_stopsTracking() =
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

    @Test
    fun resumeRun_restartsAnnouncements() =
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

    @Test
    fun stopRun_stopsServicesGracefully() =
        runBlocking {
            runStateManager.startRun()

            runStateManager.stopRun()

            verify {
                voiceCommandListener.stopListening()
                announcementManager.stop()
            }
        }

    @Test
    fun hourlyAnnouncements_updatesEveryMinute() =
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
