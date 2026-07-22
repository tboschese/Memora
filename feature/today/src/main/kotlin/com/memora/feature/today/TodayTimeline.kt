package com.memora.feature.today

/**
 * Junção pura das duas fontes do dia (falas + gaps) em uma única timeline cronológica. Sem relógio,
 * sem I/O, sem Android — determinística e trivialmente testável, no espírito de `toSegmentEntities`.
 */
object TodayTimeline {

    /**
     * Intercala [utterances] e [gaps] ordenando por [TodayItem.atMs] crescente. O critério de
     * desempate é estável e independente da ordem de entrada: a menos de [TodayItem.atMs] iguais,
     * uma [TodayItem.Utterance] vem antes de um [TodayItem.Gap]; entre itens do mesmo tipo, ordena
     * pelo fim do intervalo e depois por um id textual — nunca fica ambíguo.
     */
    fun merge(
        utterances: List<TodayItem.Utterance>,
        gaps: List<TodayItem.Gap>,
    ): List<TodayItem> =
        (utterances + gaps).sortedWith(
            compareBy(
                { it.atMs },
                { it.typeOrder() },
                { it.endMs() },
                { it.tieKey() },
            ),
        )

    /** Falas antes de gaps quando começam no mesmo instante. */
    private fun TodayItem.typeOrder(): Int = when (this) {
        is TodayItem.Utterance -> 0
        is TodayItem.Gap -> 1
    }

    private fun TodayItem.endMs(): Long = when (this) {
        is TodayItem.Utterance -> endMs
        is TodayItem.Gap -> toMs
    }

    private fun TodayItem.tieKey(): String = when (this) {
        is TodayItem.Utterance -> id
        is TodayItem.Gap -> reason.name
    }
}
