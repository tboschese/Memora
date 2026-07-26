package com.memora.core.common.timeline

/**
 * Item da timeline unificada do dia: fala transcrita, anotação do usuário ou buraco na timeline —
 * ordenáveis por [atMs]. Modelo comum em `:core:common` para que a tela unificada intercale as três
 * fontes sem que um feature dependa de outro; a composição (mapear cada fonte para cá) fica no `:app`.
 */
sealed interface DayItem {
    val atMs: Long

    data class Speech(
        val id: String,
        override val atMs: Long,
        val text: String,
        val speaker: String? = null,
        val place: String? = null,
    ) : DayItem

    data class UserNote(
        val id: String,
        override val atMs: Long,
        val text: String,
        val tags: List<String> = emptyList(),
        /** Marcada como concluída — relevante para notas de tarefa (`#tarefa`). */
        val done: Boolean = false,
    ) : DayItem

    data class Gap(
        override val atMs: Long,
        val toMs: Long,
        val reason: String,
    ) : DayItem
}

/**
 * Intercala as fontes do dia numa timeline cronológica única. Ordenação estável por [DayItem.atMs];
 * no empate, uma ordem de tipo determinística (fala, depois nota, depois gap) e uma chave textual
 * evitam ambiguidade — o mesmo conjunto sempre gera a mesma sequência.
 */
object DayTimeline {

    fun merge(vararg groups: List<DayItem>): List<DayItem> =
        groups.asSequence().flatten().sortedWith(
            compareBy({ it.atMs }, { it.typeOrder() }, { it.tieKey() }),
        ).toList()

    private fun DayItem.typeOrder(): Int = when (this) {
        is DayItem.Speech -> 0
        is DayItem.UserNote -> 1
        is DayItem.Gap -> 2
    }

    private fun DayItem.tieKey(): String = when (this) {
        is DayItem.Speech -> id
        is DayItem.UserNote -> id
        is DayItem.Gap -> "$toMs:$reason"
    }
}
