package com.memora.app.data

import com.memora.core.glossary.GlossaryCorrector
import com.memora.core.glossary.GlossaryEntry
import com.memora.core.glossary.GlossaryPrompt

/**
 * Deriva, das entradas do glossário, o que cada um dos 3 pontos de injeção precisa. Vive em `:app`
 * porque é onde o glossário encontra transcrição e digest. Reúne as peças puras de `:core:glossary`
 * (grafias canônicas, orçamento de tokens) num só lugar:
 *
 * - **ponto 1** — [whisperInitialPrompt]: `initial_prompt` do Whisper (grafias priorizadas, ≤224 tokens);
 * - **ponto 2** — a correção pós-transcrição já vive em `TranscriptResult.correctedBy` + `postProcess`;
 * - **ponto 3** — [digestTerms]: grafias canônicas para o system prompt do digest.
 */
object GlossaryInjection {

    /** Ponto 1: `initial_prompt` do Whisper, priorizando as grafias mais frequentes dentro do orçamento. */
    fun whisperInitialPrompt(
        entries: List<GlossaryEntry>,
        tokenBudget: Int = GlossaryPrompt.WHISPER_TOKEN_BUDGET,
    ): String {
        val terms = GlossaryCorrector(entries).canonicalTerms()
        return GlossaryPrompt.build(GlossaryPrompt.ranked(terms), tokenBudget)
    }

    /** Ponto 3: grafias canônicas para injetar no system prompt do digest. */
    fun digestTerms(entries: List<GlossaryEntry>): List<String> =
        GlossaryCorrector(entries).canonicalTerms()
}
