package com.memora.core.audio

import kotlin.math.sqrt

/**
 * Detecta se há fala num trecho de PCM (RF-03: VAD descarta silêncio, economizando transcrição e
 * bateria). Atrás de interface para permitir a impl real (Silero VAD via ONNX) e um baseline
 * testável sem modelo.
 */
fun interface VoiceActivityDetector {
    /** `true` se há fala provável no trecho PCM 16-bit mono. */
    fun hasSpeech(pcm: ShortArray): Boolean
}

/**
 * Baseline por energia (RMS): considera fala quando a energia do trecho passa de [rmsThreshold].
 * Não substitui o Silero (não distingue voz de ruído), mas destrava o pipeline e os testes, e serve
 * de fallback se o modelo não estiver presente (degradação graciosa).
 */
class EnergyVad(private val rmsThreshold: Double = 500.0) : VoiceActivityDetector {
    override fun hasSpeech(pcm: ShortArray): Boolean {
        if (pcm.isEmpty()) return false
        var sumSquares = 0.0
        for (sample in pcm) sumSquares += sample.toDouble() * sample.toDouble()
        val rms = sqrt(sumSquares / pcm.size)
        return rms >= rmsThreshold
    }
}
