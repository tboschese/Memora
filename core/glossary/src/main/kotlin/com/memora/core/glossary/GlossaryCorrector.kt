package com.memora.core.glossary

import java.util.Locale

/**
 * Correção determinística pós-transcrição (ponto 2 de 3 da injeção do glossário).
 * Substitui variantes erradas pela grafia canônica, respeitando limites de palavra e
 * case-insensitive, mas preservando a capitalização canônica.
 */
class GlossaryCorrector(entries: List<GlossaryEntry>) {

    // Pré-compila (variante em minúsculas) -> canônica, variantes mais longas primeiro
    // para evitar substituição parcial.
    private val replacements: List<Pair<Regex, String>> = entries
        .flatMap { entry -> entry.variants.map { it to entry.canonical } }
        .sortedByDescending { it.first.length }
        .map { (variant, canonical) ->
            Regex("\\b" + Regex.escape(variant) + "\\b", RegexOption.IGNORE_CASE) to canonical
        }

    /** Aplica todas as correções ao texto. */
    fun correct(text: String): String {
        var result = text
        for ((regex, canonical) in replacements) {
            result = regex.replace(result, canonical)
        }
        return result
    }

    /** Grafias canônicas para injeção no initial_prompt do Whisper / system prompt do digest. */
    fun canonicalTerms(): List<String> =
        replacements.map { it.second }.distinctBy { it.lowercase(Locale.ROOT) }
}
