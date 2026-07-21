package com.memora.core.common.model

import java.time.Instant

/**
 * Um trecho curto de áudio capturado (PCM 16-bit mono). É EFÊMERO: existe só entre a captura
 * e a confirmação da transcrição/embedding. Ver `EphemeralAudioStore` em :core:audio.
 *
 * O array não é copiado defensivamente aqui por custo — trate como imutável e descarte após uso.
 */
class AudioChunk(
    val id: String,
    val pcm16: ShortArray,
    val sampleRate: Int,
    val startedAt: Instant,
    val durationMs: Long,
)
