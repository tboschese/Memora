package com.memora.feature.search

import java.util.Locale

/**
 * Documento buscável — uma fala ou uma anotação achatada nos campos que a busca filtra. O adaptador
 * em `:app` projeta segmentos/notas nisto.
 */
data class SearchDocument(
    val id: String,
    val text: String,
    val timeMs: Long,
    val tags: List<String> = emptyList(),
    val speaker: String? = null,
)

/**
 * Matcher de referência (em memória) da busca: um documento casa quando contém **todos** os termos
 * (substring, case-insensitive), tem **todas** as tags pedidas e, se houver filtro de speaker, bate.
 * Resultado em ordem cronológica decrescente (mais recente primeiro). Uma query vazia não casa nada
 * — a UI mostra a timeline normal, não "tudo". Puro; o FTS do Room é só uma aceleração posterior.
 */
object SearchMatcher {

    fun match(documents: List<SearchDocument>, query: SearchQuery): List<SearchDocument> {
        if (query.isEmpty) return emptyList()
        return documents
            .filter { it.matches(query) }
            .sortedByDescending { it.timeMs }
    }

    private fun SearchDocument.matches(query: SearchQuery): Boolean {
        val haystack = text.lowercase(Locale.ROOT)
        if (query.terms.any { it !in haystack }) return false

        val docTags = tags.map { it.lowercase(Locale.ROOT) }
        if (query.tags.any { it !in docTags }) return false

        if (query.speaker != null && speaker?.lowercase(Locale.ROOT) != query.speaker) return false

        return true
    }
}
