package com.fghbuild.sidekick.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Test implementation of IVoiceCommandListener for testing without SpeechRecognizer.
 */
class FakeVoiceCommandListener : IVoiceCommandListener {
    private val _lastCommand = MutableStateFlow(VoiceCommand.NONE)
    override val lastCommand: StateFlow<VoiceCommand> = _lastCommand.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    override fun startListening() {
        _isListening.value = true
    }

    override fun stopListening() {
        _isListening.value = false
    }

    override fun destroy() {
        stopListening()
    }

    fun simulateCommand(command: VoiceCommand) {
        _lastCommand.value = command
    }
}
