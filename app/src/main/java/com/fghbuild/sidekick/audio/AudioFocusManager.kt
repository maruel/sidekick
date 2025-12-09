// Manages audio focus for speech-based features (announcements, voice recognition).
package com.fghbuild.sidekick.audio

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

class AudioFocusManager(private val audioManager: AudioManager) {
    private var audioFocusRequest: AudioFocusRequest? = null

    fun requestFocus() {
        val audioAttributes =
            AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()

        audioFocusRequest =
            AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .build()

        audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
    }

    fun abandonFocus() {
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        audioFocusRequest = null
    }
}
