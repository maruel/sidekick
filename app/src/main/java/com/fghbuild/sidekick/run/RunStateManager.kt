package com.fghbuild.sidekick.run

import com.fghbuild.sidekick.audio.AnnouncementManager
import com.fghbuild.sidekick.audio.IVoiceCommandListener
import com.fghbuild.sidekick.audio.VoiceCommand
import com.fghbuild.sidekick.data.HeartRateData
import com.fghbuild.sidekick.data.RunData
import kotlinx.coroutines.flow.StateFlow

class RunStateManager(
    private val runManager: RunManager,
    private val announcements: AnnouncementManager,
    private val voiceListener: IVoiceCommandListener,
    private val heartRateData: StateFlow<HeartRateData> = runManager.defaultHeartRateData,
) {
    private var lastKilometerAnnouncement = 0.0
    private var lastHeartRateAnnouncement = 0L

    val runData: StateFlow<RunData> = runManager.runData
    val lastCommand: StateFlow<VoiceCommand> = voiceListener.lastCommand

    fun startRun() {
        lastKilometerAnnouncement = 0.0
        lastHeartRateAnnouncement = System.currentTimeMillis()
        runManager.startRun()
        voiceListener.startListening()
    }

    fun pauseRun() {
        runManager.pauseRun()
        voiceListener.startListening()
    }

    fun resumeRun() {
        runManager.resumeRun()
        lastHeartRateAnnouncement = System.currentTimeMillis()
    }

    fun stopRun() {
        runManager.stopRun()
        voiceListener.stopListening()
        announcements.stop()
    }

    fun update() {
        val currentRunData = runData.value

        // Announce every 1 km
        val currentKm = currentRunData.distanceMeters / 1000.0
        if (currentKm >= lastKilometerAnnouncement + 1.0 && currentKm > 0) {
            announcements.speakDistance(currentKm)
            announcements.speakPace(currentRunData.paceMinPerKm)
            lastKilometerAnnouncement = (currentKm.toInt()).toDouble()
        }

        // Announce heart rate every 30 seconds when HRM is connected
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastHeartRateAnnouncement >= 30000) {
            val hrData = heartRateData.value
            if (hrData.currentBpm > 0) {
                announcements.speakHeartRate(hrData.currentBpm)
            }
            lastHeartRateAnnouncement = currentTime
        }
    }

    fun cleanup() {
        voiceListener.destroy()
        announcements.shutdown()
    }
}
