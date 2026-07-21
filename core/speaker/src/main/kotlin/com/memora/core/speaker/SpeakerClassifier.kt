package com.memora.core.speaker

import com.memora.core.common.model.AudioChunk
import com.memora.core.common.model.SpeakerDecision

/**
 * Reconhece quem fala (impl real: ECAPA-TDNN via ONNX). O embedding é calculado ANTES do
 * descarte do áudio. NUNCA chutar: abaixo do threshold ⇒ `UNKNOWN`.
 */
interface SpeakerClassifier {

    /** Gera o embedding de voz de um chunk. Roda antes do áudio ser destruído. */
    suspend fun embed(chunk: AudioChunk): FloatArray

    /** Classifica um embedding contra o perfil do dono. Função pura e determinística. */
    fun classify(embedding: FloatArray, profile: VoiceProfile): SpeakerDecision
}

/**
 * Perfil de voz do dono (gerado no enrollment). [threshold] é a similaridade mínima (cosseno)
 * para considerar SELF; abaixo dela, tratamos como OTHER/UNKNOWN conforme margem.
 */
data class VoiceProfile(
    val embedding: FloatArray,
    val threshold: Float = 0.65f,
    /** Margem abaixo do threshold em que a decisão vira UNKNOWN em vez de OTHER. */
    val unknownMargin: Float = 0.1f,
) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is VoiceProfile &&
            embedding.contentEquals(other.embedding) &&
            threshold == other.threshold &&
            unknownMargin == other.unknownMargin)

    override fun hashCode(): Int = embedding.contentHashCode() * 31 + threshold.hashCode()
}
