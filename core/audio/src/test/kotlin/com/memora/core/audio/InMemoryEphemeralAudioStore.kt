package com.memora.core.audio

import java.util.concurrent.ConcurrentHashMap

/**
 * Fake em memória do [EphemeralAudioStore]. Referência de comportamento correto do
 * contrato de efemeridade; usado pelos testes de todo o pipeline sem tocar em áudio real.
 */
class InMemoryEphemeralAudioStore : EphemeralAudioStore {

    private val store = ConcurrentHashMap<String, ByteArray>()

    override suspend fun write(chunkId: String, pcm: ByteArray) {
        store[chunkId] = pcm.copyOf()
    }

    override suspend fun read(chunkId: String): ByteArray? = store[chunkId]?.copyOf()

    override suspend fun destroy(chunkId: String) {
        // Sobrescreve antes de remover (nunca deixar o conteúdo recuperável).
        store[chunkId]?.fill(0)
        store.remove(chunkId)
    }

    override suspend fun activeChunkIds(): Set<String> = store.keys.toSet()
}
