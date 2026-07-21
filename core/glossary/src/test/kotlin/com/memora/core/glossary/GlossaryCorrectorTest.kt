package com.memora.core.glossary

import org.junit.Assert.assertEquals
import org.junit.Test

class GlossaryCorrectorTest {

    private val corrector = GlossaryCorrector(
        listOf(
            GlossaryEntry(id = "1", canonical = "Kubernetes", variants = listOf("kubernetis", "cubernetes")),
            GlossaryEntry(id = "2", canonical = "Thiago", variants = listOf("tiago")),
        ),
    )

    @Test
    fun `substitui variante por grafia canonica preservando capitalizacao`() {
        assertEquals(
            "Subimos no Kubernetes com o Thiago",
            corrector.correct("Subimos no cubernetes com o tiago"),
        )
    }

    @Test
    fun `respeita limites de palavra`() {
        // "tiagonel" não deve virar "Thiagonel"
        assertEquals("tiagonel", corrector.correct("tiagonel"))
    }

    @Test
    fun `termos canonicos para injecao no prompt`() {
        assertEquals(listOf("Kubernetes", "Thiago"), corrector.canonicalTerms())
    }
}
