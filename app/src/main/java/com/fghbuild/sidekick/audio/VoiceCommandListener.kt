// Speech recognition listener for voice commands (pause, resume) with audio focus management.
package com.fghbuild.sidekick.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VoiceCommand {
    PAUSE,
    RESUME,
    NONE,
}

interface IVoiceCommandListener {
    val lastCommand: StateFlow<VoiceCommand>
    val isListening: StateFlow<Boolean>

    fun startListening()

    fun stopListening()

    fun destroy()
}

class VoiceCommandListener(private val context: Context) : RecognitionListener, IVoiceCommandListener {
    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _lastCommand = MutableStateFlow(VoiceCommand.NONE)
    override val lastCommand: StateFlow<VoiceCommand> = _lastCommand.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var audioFocusRequest: AudioFocusRequest? = null

    override fun startListening() {
        if (_isListening.value) return

        try {
            if (
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            requestAudioFocus()

            _isListening.value = true
            val intent =
                Intent(
                    "android.speech.action.RECOGNIZE_SPEECH",
                ).apply {
                    putExtra(
                        "android.speech.extra.LANGUAGE_MODEL",
                        "free_form",
                    )
                    putExtra("android.speech.extra.CALLING_PACKAGE", context.packageName)
                }
            speechRecognizer.setRecognitionListener(this)
            speechRecognizer.startListening(intent)
        } catch (_: SecurityException) {
            // Permission was revoked at runtime
            _isListening.value = false
        } catch (_: IllegalArgumentException) {
            // Invalid intent configuration
            _isListening.value = false
        }
    }

    override fun stopListening() {
        speechRecognizer.stopListening()
        _isListening.value = false
        abandonAudioFocus()
    }

    private fun requestAudioFocus() {
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

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        audioFocusRequest = null
    }

    override fun onReadyForSpeech(params: android.os.Bundle?) {
        // Ready for speech
    }

    override fun onBeginningOfSpeech() {
        // Speech has started
    }

    override fun onRmsChanged(rmsdB: Float) {
        // RMS changed
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        // Buffer received
    }

    override fun onEndOfSpeech() {
        // Speech ended
    }

    override fun onError(error: Int) {
        _isListening.value = false
        abandonAudioFocus()
    }

    override fun onResults(results: android.os.Bundle?) {
        _isListening.value = false
        abandonAudioFocus()

        results?.let {
            val matches =
                it.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList()
            if (matches.isNotEmpty()) {
                val topResult = matches[0].lowercase()
                _lastCommand.value =
                    when {
                        topResult.contains("pause") -> VoiceCommand.PAUSE
                        topResult.contains("resume") -> VoiceCommand.RESUME
                        else -> VoiceCommand.NONE
                    }
            }
        }

        // Restart listening
        startListening()
    }

    override fun onPartialResults(partialResults: android.os.Bundle?) {
        // Partial results
    }

    override fun onEvent(
        eventType: Int,
        params: android.os.Bundle?,
    ) {
        // Event occurred
    }

    override fun destroy() {
        speechRecognizer.destroy()
    }
}
