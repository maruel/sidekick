// Text-to-speech announcements for run metrics (distance, pace, heart rate) with audio focus management.
package com.fghbuild.sidekick.audio

import android.content.Context
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AnnouncementManager(private val context: Context) : TextToSpeech.OnInitListener {
    private val textToSpeech = TextToSpeech(context, this)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val audioFocusManager = AudioFocusManager(audioManager)

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        textToSpeech.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    // Speech started
                }

                override fun onDone(utteranceId: String?) {
                    audioFocusManager.abandonFocus()
                }

                override fun onError(utteranceId: String?) {
                    audioFocusManager.abandonFocus()
                }
            },
        )
    }

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
        audioFocusManager.requestFocus()
        val params = hashMapOf(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID to "announcement")
        @Suppress("DEPRECATION")
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params)
    }

    fun stop() {
        textToSpeech.stop()
        audioFocusManager.abandonFocus()
    }

    fun shutdown() {
        textToSpeech.shutdown()
        audioFocusManager.abandonFocus()
    }
}
