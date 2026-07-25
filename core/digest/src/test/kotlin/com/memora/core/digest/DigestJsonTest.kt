package com.memora.core.digest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** org.json roda com implementação real sob Robolectric (nos unit tests puros, ele lança stubs). */
@RunWith(RobolectricTestRunner::class)
class DigestJsonTest {

    @Test
    fun `parses a well-formed digest`() {
        val json = """
            {
              "summary": "Dia produtivo.",
              "decisions": ["Adotar Kotlin", "Cancelar reunião"],
              "myActionItems": ["Responder e-mail"],
              "themes": ["trabalho", "estudo"]
            }
        """.trimIndent()

        val digest = DigestJson.parse(epochDay = 42, json = json)!!
        assertEquals(42L, digest.epochDay)
        assertEquals("Dia produtivo.", digest.summary)
        assertEquals(listOf("Adotar Kotlin", "Cancelar reunião"), digest.decisions)
        assertEquals(listOf("Responder e-mail"), digest.myActionItems)
        assertEquals(listOf("trabalho", "estudo"), digest.themes)
    }

    @Test
    fun `missing lists default to empty`() {
        val digest = DigestJson.parse(1, """{"summary": "Só resumo."}""")!!
        assertTrue(digest.decisions.isEmpty())
        assertTrue(digest.myActionItems.isEmpty())
        assertTrue(digest.themes.isEmpty())
    }

    @Test
    fun `malformed json is rejected`() {
        assertNull(DigestJson.parse(1, "isto não é json"))
        assertNull(DigestJson.parse(1, """{"summary": "x" """)) // chave não fechada
    }

    @Test
    fun `a missing or blank summary is rejected`() {
        assertNull(DigestJson.parse(1, """{"decisions": ["x"]}"""))
        assertNull(DigestJson.parse(1, """{"summary": "   "}"""))
    }

    @Test
    fun `non-string list items are discarded and strings are trimmed`() {
        val json = """{"summary": "s", "themes": ["  ok  ", 42, null, "", "bom"]}"""
        val digest = DigestJson.parse(1, json)!!
        assertEquals(listOf("ok", "bom"), digest.themes)
    }

    @Test
    fun `extra fields are ignored and epochDay comes from the caller`() {
        val json = """{"summary": "s", "epochDay": 999, "lixo": {"a": 1}}"""
        val digest = DigestJson.parse(epochDay = 7, json = json)!!
        assertEquals(7L, digest.epochDay) // ignora o epochDay do modelo
    }

    @Test
    fun `a non-array list field is treated as empty`() {
        val digest = DigestJson.parse(1, """{"summary": "s", "themes": "não é array"}""")!!
        assertTrue(digest.themes.isEmpty())
    }
}
