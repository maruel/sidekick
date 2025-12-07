package com.fghbuild.sidekick.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AnnouncementManager(private val context: Context) : TextToSpeech.OnInitListener {
    private val textToSpeech = TextToSpeech(context, this)

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            _isReady.value = true
        }
    }

    fun speakDistance(distanceKm: Double) {
        if (!_isReady.value) return
        speak(context.getString(com.fghbuild.sidekick.R.string.announcement_distance, distanceKm))
    }

    fun speakPace(paceMinPerKm: Double) {
        if (!_isReady.value) return
        val minutes = paceMinPerKm.toInt()
        val seconds = ((paceMinPerKm - minutes) * 60).toInt()
        speak(context.getString(com.fghbuild.sidekick.R.string.announcement_pace, minutes, seconds))
    }

    fun speakHeartRate(bpm: Int) {
        if (!_isReady.value) return
        speak(context.getString(com.fghbuild.sidekick.R.string.announcement_heart_rate, bpm))
    }

    fun speakAverageHeartRate(bpm: Int) {
        if (!_isReady.value) return
        speak(context.getString(com.fghbuild.sidekick.R.string.announcement_average_heart_rate, bpm))
    }

    private fun speak(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null)
    }

    fun stop() {
        textToSpeech.stop()
    }

    fun shutdown() {
        textToSpeech.shutdown()
    }
}
