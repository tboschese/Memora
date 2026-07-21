package com.memora.core.audio

/**
 * Fatiador de PCM em chunks de duração fixa (RF na §4.2: chunking 30–60s + buffer efêmero).
 *
 * Recebe amostras PCM 16-bit mono via [feed] e emite chunks completos assim que acumula
 * [samplesPerChunk]. O que sobra fica no buffer até o próximo [feed] ou até um [flush] (ex.: no
 * stop da captura). A perda máxima em crash é limitada pelo tamanho do chunk (o serviço faz flush
 * periódico). Lógica pura e determinística — não conhece tempo, id nem storage: o serviço embrulha
 * cada `ShortArray` num `AudioChunk` (id + `startedAt`) e o entrega ao `EphemeralAudioStore`.
 */
class PcmChunker(val sampleRate: Int, targetMs: Long) {

    /** Amostras por chunk = sampleRate × (targetMs / 1000). */
    val samplesPerChunk: Int = ((sampleRate.toLong() * targetMs) / 1000L).toInt()

    private var buffer = ShortArray(samplesPerChunk)
    private var size = 0

    init {
        require(sampleRate > 0) { "sampleRate deve ser > 0" }
        require(samplesPerChunk > 0) { "targetMs curto demais para o sampleRate" }
    }

    /** Alimenta amostras e retorna os chunks completados (pode ser 0, 1 ou vários). */
    fun feed(samples: ShortArray): List<ShortArray> {
        val out = ArrayList<ShortArray>()
        var offset = 0
        while (offset < samples.size) {
            val toCopy = minOf(samples.size - offset, samplesPerChunk - size)
            System.arraycopy(samples, offset, buffer, size, toCopy)
            size += toCopy
            offset += toCopy
            if (size == samplesPerChunk) {
                out.add(buffer.copyOf())
                size = 0
            }
        }
        return out
    }

    /** Emite o resto acumulado (chunk parcial), ou `null` se o buffer estiver vazio. */
    fun flush(): ShortArray? =
        if (size == 0) null else buffer.copyOf(size).also { size = 0 }

    /** Duração (ms) do áudio ainda no buffer, não emitido. */
    fun pendingMs(): Long = durationMsOf(size)

    /** Converte uma contagem de amostras em milissegundos, no sample rate deste chunker. */
    fun durationMsOf(sampleCount: Int): Long = sampleCount.toLong() * 1000L / sampleRate
}
