package com.memora.core.glossary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GlossaryPromptTest {

    /** Contador simples para asserts determinísticos: 1 token por termo. */
    private val oneTokenEach: (String) -> Int = { 1 }

    @Test
    fun `orders by weight descending`() {
        val prompt = GlossaryPrompt.build(
            listOf(
                WeightedTerm("Kubernetes", weight = 1),
                WeightedTerm("Memora", weight = 10),
                WeightedTerm("Anthropic", weight = 5),
            ),
            tokenCounter = oneTokenEach,
        )
        assertEquals("Memora, Anthropic, Kubernetes", prompt)
    }

    @Test
    fun `stops at the token budget, keeping the most frequent`() {
        // orçamento 3: "Memora"(1) + sep(1) + "Anthropic"(1) = 3; "Kubernetes" não cabe.
        val prompt = GlossaryPrompt.build(
            listOf(
                WeightedTerm("Kubernetes", weight = 1),
                WeightedTerm("Memora", weight = 10),
                WeightedTerm("Anthropic", weight = 5),
            ),
            tokenBudget = 3,
            tokenCounter = oneTokenEach,
        )
        assertEquals("Memora, Anthropic", prompt)
    }

    @Test
    fun `skips an oversized term but still fits a smaller one below it`() {
        // "grande" custa 5, não cabe em 3; "ok" custa 1 e entra.
        val counter: (String) -> Int = { if (it == "grande") 5 else 1 }
        val prompt = GlossaryPrompt.build(
            listOf(WeightedTerm("grande", weight = 10), WeightedTerm("ok", weight = 1)),
            tokenBudget = 3,
            tokenCounter = counter,
        )
        assertEquals("ok", prompt)
    }

    @Test
    fun `deduplicates case-insensitively keeping the highest weight`() {
        val prompt = GlossaryPrompt.build(
            listOf(WeightedTerm("memora", weight = 1), WeightedTerm("Memora", weight = 9)),
            tokenCounter = oneTokenEach,
        )
        assertEquals("Memora", prompt)
    }

    @Test
    fun `blanks are ignored`() {
        val prompt = GlossaryPrompt.build(
            listOf(WeightedTerm("  ", weight = 10), WeightedTerm("Memora", weight = 1)),
            tokenCounter = oneTokenEach,
        )
        assertEquals("Memora", prompt)
    }

    @Test
    fun `empty input yields empty prompt`() {
        assertEquals("", GlossaryPrompt.build(emptyList(), tokenCounter = oneTokenEach))
    }

    @Test
    fun `zero budget yields empty prompt`() {
        val prompt = GlossaryPrompt.build(
            listOf(WeightedTerm("Memora", weight = 1)),
            tokenBudget = 0,
            tokenCounter = oneTokenEach,
        )
        assertEquals("", prompt)
    }

    @Test
    fun `negative budget is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            GlossaryPrompt.build(listOf(WeightedTerm("x", 1)), tokenBudget = -1)
        }
    }

    @Test
    fun `estimateTokens grows with length and is at least one`() {
        assertEquals(1, GlossaryPrompt.estimateTokens("ab"))
        assertEquals(0, GlossaryPrompt.estimateTokens("   "))
        assertTrue(GlossaryPrompt.estimateTokens("Kubernetes") > GlossaryPrompt.estimateTokens("Go"))
    }

    @Test
    fun `ranked preserves input order as descending priority`() {
        val prompt = GlossaryPrompt.build(
            GlossaryPrompt.ranked(listOf("primeiro", "segundo", "terceiro")),
            tokenCounter = oneTokenEach,
        )
        assertEquals("primeiro, segundo, terceiro", prompt)
    }

    @Test
    fun `default estimate respects the whisper budget`() {
        // Muitos termos longos: o prompt real não deve estourar ~224 tokens.
        val terms = (1..500).map { WeightedTerm("TermoLongoNumero$it", weight = it) }
        val prompt = GlossaryPrompt.build(terms)
        val tokens = prompt.split(", ").sumOf { GlossaryPrompt.estimateTokens(it) } +
            (prompt.split(", ").size - 1) * GlossaryPrompt.SEPARATOR_TOKENS
        assertTrue("tokens=$tokens", tokens <= GlossaryPrompt.WHISPER_TOKEN_BUDGET)
    }
}
