package com.fghbuild.sidekick.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AnnouncementManager(context: Context) : TextToSpeech.OnInitListener {
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

        val distance = String.format("%.1f", distanceKm)
        val text = "Distance: $distance kilometers"
        speak(text)
    }

    fun speakPace(paceMinPerKm: Double) {
        if (!_isReady.value) return

        val minutes = paceMinPerKm.toInt()
        val seconds = ((paceMinPerKm - minutes) * 60).toInt()
        val text = "Pace: $minutes minutes $seconds seconds per kilometer"
        speak(text)
    }

    fun speakHeartRate(bpm: Int) {
        if (!_isReady.value) return

        val text = "Heart rate: $bpm beats per minute"
        speak(text)
    }

    fun speakAverageHeartRate(bpm: Int) {
        if (!_isReady.value) return

        val text = "Average heart rate: $bpm beats per minute"
        speak(text)
    }

    private fun speak(text: String) {
        try {
            @Suppress("DEPRECATION")
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            textToSpeech.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shutdown() {
        try {
            textToSpeech.shutdown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
