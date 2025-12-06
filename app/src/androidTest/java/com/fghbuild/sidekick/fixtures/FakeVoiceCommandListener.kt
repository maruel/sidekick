package com.fghbuild.sidekick.fixtures

import com.fghbuild.sidekick.audio.VoiceCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake VoiceCommandListener for testing.
 * Simulates voice command detection without requiring SpeechRecognizer.
 * This does NOT extend VoiceCommandListener to avoid instantiation issues in tests.
 * Tests should use this with RunStateManager by passing it directly.
 */
class FakeVoiceCommandListener {
    private val _lastCommand = MutableStateFlow(VoiceCommand.NONE)
    private val _isListening = MutableStateFlow(false)

    val lastCommand: StateFlow<VoiceCommand> = _lastCommand.asStateFlow()
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    fun startListening() {
        _isListening.value = true
    }

    fun stopListening() {
        _isListening.value = false
    }

    fun destroy() {
        // No-op in fake
    }

    fun simulateCommand(command: VoiceCommand) {
        _lastCommand.value = command
    }

    fun reset() {
        _lastCommand.value = VoiceCommand.NONE
    }
}
