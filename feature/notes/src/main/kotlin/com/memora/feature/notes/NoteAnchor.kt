package com.memora.feature.notes

/** Segmento candidato a âncora de uma nota: id + intervalo de tempo `[startMs, endMs]`. */
data class AnchorableSegment(val id: String, val startMs: Long, val endMs: Long)

/**
 * Ancora uma anotação ao segmento vigente no instante em que foi feita (RF-07): o "timestamp exato"
 * que liga a nota ao que estava sendo falado. Puro e determinístico.
 */
object NoteAnchor {

    /**
     * O id do segmento a ancorar para uma nota em [noteTimeMs]:
     * - o segmento cujo intervalo contém o instante; senão
     * - o mais recente que já havia começado (o que estava em curso/acabara de terminar); senão
     * - `null` (nota antes de qualquer fala — nota avulsa).
     */
    fun anchorFor(noteTimeMs: Long, segments: List<AnchorableSegment>): String? {
        segments.firstOrNull { noteTimeMs in it.startMs..it.endMs }?.let { return it.id }
        return segments
            .filter { it.startMs <= noteTimeMs }
            .maxByOrNull { it.startMs }
            ?.id
    }
}
