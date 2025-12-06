package com.fghbuild.sidekick.fixtures

import com.fghbuild.sidekick.audio.VoiceCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake VoiceCommandListener for testing.
 * Simulates voice command detection without requiring SpeechRecognizer.
 */
class FakeVoiceCommandListener {
    private val _lastCommand = MutableStateFlow(VoiceCommand.NONE)

    val lastCommand: StateFlow<VoiceCommand> = _lastCommand.asStateFlow()

    fun startListening() {
        // No-op in fake
    }

    fun stopListening() {
        // No-op in fake
    }

    fun simulateCommand(command: VoiceCommand) {
        _lastCommand.value = command
    }

    fun reset() {
        _lastCommand.value = VoiceCommand.NONE
    }
}
