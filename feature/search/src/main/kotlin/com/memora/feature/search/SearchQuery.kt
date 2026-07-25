package com.memora.feature.search

import java.util.Locale

/**
 * Query de busca já estruturada: termos livres (todos precisam casar) + filtros por [tags] e
 * [speaker]. O FTS do Room acelera a busca depois; este é o **contrato** de o que casa — puro e
 * testável, e também o matcher de referência em memória ([SearchMatcher]).
 */
data class SearchQuery(
    val terms: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val speaker: String? = null,
) {
    val isEmpty: Boolean get() = terms.isEmpty() && tags.isEmpty() && speaker == null
}

/**
 * Parseia a caixa de busca em [SearchQuery]. Sintaxe leve, na ordem que o usuário digita:
 * - `#tag` → filtro de tag;
 * - `@speaker` → filtro de quem falou (ex.: `@self`);
 * - qualquer outro token → termo livre.
 *
 * Tudo é normalizado (trim + lowercase) e deduplicado; um `#`/`@` solto é ignorado.
 */
object SearchQueryParser {

    fun parse(raw: String): SearchQuery {
        val terms = LinkedHashSet<String>()
        val tags = LinkedHashSet<String>()
        var speaker: String? = null

        for (rawToken in raw.trim().split(Regex("\\s+"))) {
            val token = rawToken.lowercase(Locale.ROOT)
            when {
                token.length <= 1 && (token == "#" || token == "@") -> Unit // marcador solto
                token.isEmpty() -> Unit
                token.startsWith("#") -> tags += token.removePrefix("#")
                token.startsWith("@") -> speaker = token.removePrefix("@")
                else -> terms += token
            }
        }
        return SearchQuery(terms.toList(), tags.toList(), speaker)
    }
}
