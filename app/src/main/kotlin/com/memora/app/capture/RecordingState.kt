package com.memora.app.capture

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estado de gravação compartilhado entre o [CaptureService] (fonte de verdade) e a UI (via
 * `CaptureController`). Singleton para que ambos observem os mesmos `StateFlow`s. Os contadores dão
 * feedback visível: quantos trechos com fala foram capturados e quantos o VAD descartou.
 */
@Singleton
class RecordingState @Inject constructor() {
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _captured = MutableStateFlow(0)
    val captured: StateFlow<Int> = _captured.asStateFlow()

    private val _dropped = MutableStateFlow(0)
    val dropped: StateFlow<Int> = _dropped.asStateFlow()

    fun startSession() {
        _captured.value = 0
        _dropped.value = 0
        _isRecording.value = true
    }

    fun set(recording: Boolean) {
        _isRecording.value = recording
    }

    fun onCaptured() {
        _captured.value += 1
    }

    fun setDropped(count: Int) {
        _dropped.value = count
    }
}
