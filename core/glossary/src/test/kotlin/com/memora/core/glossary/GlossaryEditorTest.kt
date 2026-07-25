package com.memora.core.glossary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GlossaryEditorTest {

    private val ids = generateSequence(1) { it + 1 }.map { "g$it" }.iterator()
    private val newId = { ids.next() }

    @Test
    fun `learns a brand-new entry when the canonical is unknown`() {
        val result = GlossaryEditor.learnCorrection(emptyList(), "kubernetis", "Kubernetes", newId)

        assertEquals(1, result.size)
        assertEquals("Kubernetes", result.single().canonical)
        assertEquals(listOf("kubernetis"), result.single().variants)
    }

    @Test
    fun `appends a variant to an existing entry`() {
        val entries = listOf(GlossaryEntry(id = "g0", canonical = "Kubernetes", variants = listOf("kubernetis")))

        val result = GlossaryEditor.learnCorrection(entries, "cubernetes", "kubernetes", newId)

        assertEquals(1, result.size)
        assertEquals(listOf("kubernetis", "cubernetes"), result.single().variants)
        assertEquals("g0", result.single().id) // mesma entrada, atualizada
    }

    @Test
    fun `does not duplicate a variant already present`() {
        val entries = listOf(GlossaryEntry(id = "g0", canonical = "Kubernetes", variants = listOf("kubernetis")))

        val result = GlossaryEditor.learnCorrection(entries, "KUBERNETIS", "Kubernetes", newId)

        assertSame(entries, result) // inalterado
    }

    @Test
    fun `variant equal to the canonical is a no-op`() {
        val result = GlossaryEditor.learnCorrection(emptyList(), "Memora", "memora", newId)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `blank inputs are ignored`() {
        assertTrue(GlossaryEditor.learnCorrection(emptyList(), "  ", "Memora", newId).isEmpty())
        assertTrue(GlossaryEditor.learnCorrection(emptyList(), "memra", "  ", newId).isEmpty())
    }

    @Test
    fun `inputs are trimmed`() {
        val result = GlossaryEditor.learnCorrection(emptyList(), "  memra  ", "  Memora  ", newId)
        assertEquals("Memora", result.single().canonical)
        assertEquals(listOf("memra"), result.single().variants)
    }

    @Test
    fun `the learned correction is then applied by the corrector`() {
        val entries = GlossaryEditor.learnCorrection(emptyList(), "memra", "Memora", newId)
        val corrected = GlossaryCorrector(entries).correct("abri o memra hoje")
        assertEquals("abri o Memora hoje", corrected)
    }
}
