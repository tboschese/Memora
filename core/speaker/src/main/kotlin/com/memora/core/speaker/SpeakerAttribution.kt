package com.memora.core.speaker

import com.memora.core.common.model.SpeakerDecision
import com.memora.core.common.model.SpeakerLabel

/** Embedding de voz de um segmento, calculado a partir do áudio ANTES do descarte (regra 5). */
data class SegmentEmbedding(val segmentId: String, val embedding: FloatArray) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is SegmentEmbedding && segmentId == other.segmentId && embedding.contentEquals(other.embedding))

    override fun hashCode(): Int = segmentId.hashCode() * 31 + embedding.contentHashCode()
}

/** Atribuição resultante para um segmento: o rótulo e a similaridade que o gerou. */
data class SpeakerAssignment(val segmentId: String, val decision: SpeakerDecision)

/**
 * Atribui speaker a vários segmentos de uma vez (§5.2), centralizando a **regra 5**: sem um
 * [VoiceProfile] (enrollment ainda não feito), NADA é chutado — tudo vira `UNKNOWN`. Com perfil,
 * cada embedding passa por [decideSpeaker]. Puro: os embeddings vêm do modelo, calculados antes do
 * áudio ser destruído.
 */
object SpeakerAttribution {

    fun attribute(segments: List<SegmentEmbedding>, profile: VoiceProfile?): List<SpeakerAssignment> =
        segments.map { segment ->
            val decision = if (profile == null) {
                SpeakerDecision(SpeakerLabel.UNKNOWN, similarity = 0f)
            } else {
                decideSpeaker(segment.embedding, profile)
            }
            SpeakerAssignment(segment.segmentId, decision)
        }
}
