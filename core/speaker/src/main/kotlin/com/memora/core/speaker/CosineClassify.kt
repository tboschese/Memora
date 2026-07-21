package com.memora.core.speaker

import com.memora.core.common.model.SpeakerDecision
import com.memora.core.common.model.SpeakerLabel
import kotlin.math.sqrt

/** Similaridade de cosseno entre dois embeddings. Retorna 0f se algum vetor for nulo. */
fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
    require(a.size == b.size) { "embeddings de tamanhos diferentes: ${a.size} vs ${b.size}" }
    var dot = 0f
    var normA = 0f
    var normB = 0f
    for (i in a.indices) {
        dot += a[i] * b[i]
        normA += a[i] * a[i]
        normB += b[i] * b[i]
    }
    if (normA == 0f || normB == 0f) return 0f
    return dot / (sqrt(normA) * sqrt(normB))
}

/**
 * Regra de decisão compartilhada (pura). Acima do threshold ⇒ SELF. Logo abaixo (dentro da
 * margem) ⇒ UNKNOWN (não chutar). Bem abaixo ⇒ OTHER.
 */
fun decideSpeaker(embedding: FloatArray, profile: VoiceProfile): SpeakerDecision {
    val sim = cosineSimilarity(embedding, profile.embedding)
    val label = when {
        sim >= profile.threshold -> SpeakerLabel.SELF
        sim >= profile.threshold - profile.unknownMargin -> SpeakerLabel.UNKNOWN
        else -> SpeakerLabel.OTHER
    }
    return SpeakerDecision(label = label, similarity = sim)
}
