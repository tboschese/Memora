package com.memora.core.glossary

import java.util.Locale
import kotlin.math.ceil

/** Termo do glossário com um peso de prioridade (ex.: frequência de uso). Maior peso entra antes. */
data class WeightedTerm(val term: String, val weight: Int)

/**
 * Ponto 1 de 3 da injeção do glossário: monta o `initial_prompt` do Whisper a partir das grafias
 * canônicas, dentro de um orçamento de tokens ([WHISPER_TOKEN_BUDGET] ~224) e **priorizando os mais
 * frequentes** (RF-31). Guiar o Whisper com os termos certos reduz erro em nomes próprios/jargão.
 *
 * A contagem de tokens é um *seam*: [estimateTokens] é uma heurística conservadora (sem o tokenizer
 * real em mãos); a impl que embute o Whisper pode injetar o BPE verdadeiro em [tokenCounter]. Pura e
 * determinística — nada de rede, device ou modelo.
 */
object GlossaryPrompt {
    const val WHISPER_TOKEN_BUDGET = 224

    /** Tokens reservados por separador entre termos (a vírgula, no BPE típico). */
    const val SEPARATOR_TOKENS = 1

    private const val SEPARATOR = ", "

    /**
     * Estimativa conservadora de tokens de um termo. Nomes próprios/jargão fragmentam mais no BPE,
     * então usamos ~1 token a cada 3 caracteres (mínimo 1). Substituível pelo tokenizer real.
     */
    fun estimateTokens(term: String): Int =
        if (term.isBlank()) 0 else maxOf(1, ceil(term.trim().length / 3.0).toInt())

    /**
     * Seleciona, em ordem de prioridade (peso desc), os termos que cabem em [tokenBudget] contando
     * também os separadores, e os junta no `initial_prompt`. Um termo que não cabe é pulado — um
     * termo menor logo abaixo ainda pode entrar. Deduplica por grafia (case-insensitive), mantendo
     * a de maior peso. Retorna "" se nada couber.
     */
    fun build(
        terms: List<WeightedTerm>,
        tokenBudget: Int = WHISPER_TOKEN_BUDGET,
        tokenCounter: (String) -> Int = ::estimateTokens,
    ): String {
        require(tokenBudget >= 0) { "tokenBudget não pode ser negativo: $tokenBudget" }
        val ordered = terms
            .filter { it.term.isNotBlank() }
            .sortedByDescending { it.weight } // estável: empate preserva a ordem de entrada
            .distinctBy { it.term.trim().lowercase(Locale.ROOT) }

        val chosen = mutableListOf<String>()
        var used = 0
        for (t in ordered) {
            val term = t.term.trim()
            val cost = tokenCounter(term) + if (chosen.isEmpty()) 0 else SEPARATOR_TOKENS
            if (used + cost <= tokenBudget) {
                chosen += term
                used += cost
            }
        }
        return chosen.joinToString(SEPARATOR)
    }

    /**
     * Pondera uma lista já em ordem de prioridade (o primeiro é o mais importante) para alimentar
     * [build] — ex.: `build(ranked(corrector.canonicalTerms()))`.
     */
    fun ranked(terms: List<String>): List<WeightedTerm> =
        terms.mapIndexed { i, term -> WeightedTerm(term, weight = terms.size - i) }
}
