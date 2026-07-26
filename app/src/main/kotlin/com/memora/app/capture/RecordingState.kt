package com.memora.app.capture

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estado de gravação compartilhado entre o [CaptureService] (fonte de verdade) e a UI (via
 * `CaptureController`). Singleton para que ambos observem o mesmo `StateFlow`.
 */
@Singleton
class RecordingState @Inject constructor() {
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    fun set(recording: Boolean) {
        _isRecording.value = recording
    }
}
