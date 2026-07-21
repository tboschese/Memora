package com.memora.core.audio

/**
 * Áudio é efêmero (default de privacidade).
 *
 * Todo áudio capturado vive somente entre a captura e a confirmação da transcrição — não
 * guardamos PCM cru por padrão (economiza storage e reduz dado sensível em repouso).
 * Nenhuma implementação mantém o PCM depois de [destroy]; a destruição é irreversível e
 * remove qualquer resíduo (memória e, na impl real, disco temporário).
 *
 * O contrato é validado por [com.memora.core.audio.EphemeralAudioContractTest] contra um
 * fake em memória. A impl real (arquivo temporário criptografado) da Fase 1 herda o mesmo
 * contrato e ganha, adicionalmente, a prova de ausência de resíduo em disco.
 */
interface EphemeralAudioStore {

    /** Guarda um chunk de PCM sob um id efêmero. */
    suspend fun write(chunkId: String, pcm: ByteArray)

    /** Lê o chunk para transcrição, ou null se já destruído. */
    suspend fun read(chunkId: String): ByteArray?

    /** Destrói o chunk de forma irreversível. Idempotente. */
    suspend fun destroy(chunkId: String)

    /** Ids ainda vivos (não destruídos). Usado para provar que nada vaza. */
    suspend fun activeChunkIds(): Set<String>
}
