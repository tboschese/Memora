package com.memora.core.location

/** Lugar resolvido num instante — a saída do [GeocodingProvider] amostrada ao longo do dia. */
data class PlaceSample(val timeMs: Long, val place: String?)

/**
 * Intervalo contíguo em que um lugar (ou nenhum, quando [place] é null) esteve vigente. [toMs] é
 * exclusivo; `null` marca o intervalo ainda aberto (o último).
 */
data class PlaceInterval(val fromMs: Long, val toMs: Long?, val place: String?)

/**
 * Constrói o "lugar vigente" ao longo do tempo a partir das amostras de localização (RF-29): colapsa
 * amostras num histórico de intervalos e serve de lookup para segmentos/anotações herdarem o lugar
 * do seu instante (RF-30). Puro e determinístico — a coordenada→lugar é do [GeocodingProvider].
 *
 * Aplica **histerese**: uma troca de lugar só vale após [minConfirmations] amostras consecutivas no
 * novo lugar, evitando que uma leitura espúria na borda de um raio fragmente a timeline. O novo
 * intervalo começa na primeira amostra do novo lugar, não na que o confirmou.
 */
object PlaceTimeline {

    fun build(samples: List<PlaceSample>, minConfirmations: Int = 1): List<PlaceInterval> {
        require(minConfirmations >= 1) { "minConfirmations deve ser >= 1, veio $minConfirmations" }
        val sorted = samples.sortedBy { it.timeMs }
        if (sorted.isEmpty()) return emptyList()

        val intervals = mutableListOf<PlaceInterval>()
        var currentPlace = sorted.first().place
        var currentFrom = sorted.first().timeMs
        var candidate: String? = null
        var candidateFrom = 0L
        var candidateCount = 0

        for (sample in sorted.drop(1)) {
            if (samePlace(sample.place, currentPlace)) {
                candidate = null
                candidateCount = 0
                continue
            }
            if (candidateCount > 0 && samePlace(sample.place, candidate)) {
                candidateCount++
            } else {
                candidate = sample.place
                candidateFrom = sample.timeMs
                candidateCount = 1
            }
            if (candidateCount >= minConfirmations) {
                intervals += PlaceInterval(currentFrom, candidateFrom, currentPlace)
                currentPlace = candidate
                currentFrom = candidateFrom
                candidate = null
                candidateCount = 0
            }
        }
        intervals += PlaceInterval(currentFrom, null, currentPlace)
        return intervals
    }

    /** Lugar vigente em [timeMs]; `null` antes da primeira amostra ou num trecho sem lugar. */
    fun placeAt(intervals: List<PlaceInterval>, timeMs: Long): String? =
        intervals.firstOrNull { timeMs >= it.fromMs && (it.toMs == null || timeMs < it.toMs) }?.place

    private fun samePlace(a: String?, b: String?): Boolean = a == b
}
