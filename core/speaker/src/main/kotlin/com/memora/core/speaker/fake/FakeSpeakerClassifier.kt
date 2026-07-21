package com.memora.core.speaker.fake

import com.memora.core.common.model.AudioChunk
import com.memora.core.common.model.SpeakerDecision
import com.memora.core.speaker.SpeakerClassifier
import com.memora.core.speaker.VoiceProfile
import com.memora.core.speaker.decideSpeaker

/**
 * Fake do [SpeakerClassifier]. `embed` devolve embeddings programados por chunkId (ou um vetor
 * derivado do id, determinístico). `classify` usa a MESMA regra de cosseno da produção
 * ([decideSpeaker]), então testes de atribuição são fiéis sem precisar do modelo ONNX.
 */
class FakeSpeakerClassifier(
    private val embeddings: Map<String, FloatArray> = emptyMap(),
    private val dimension: Int = 16,
) : SpeakerClassifier {

    override suspend fun embed(chunk: AudioChunk): FloatArray =
        embeddings[chunk.id] ?: deterministicEmbedding(chunk.id, dimension)

    override fun classify(embedding: FloatArray, profile: VoiceProfile): SpeakerDecision =
        decideSpeaker(embedding, profile)

    private fun deterministicEmbedding(seed: String, dim: Int): FloatArray {
        val rnd = java.util.Random(seed.hashCode().toLong())
        return FloatArray(dim) { rnd.nextFloat() }
    }
}
