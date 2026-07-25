package com.memora.feature.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteInputTest {

    @Test
    fun `splits body from trailing tag`() {
        val draft = NoteInput.parse("comprar pão #tarefa")
        assertEquals("comprar pão", draft.text)
        assertEquals(listOf("tarefa"), draft.tags)
    }

    @Test
    fun `tags can appear anywhere and are normalized and deduped`() {
        val draft = NoteInput.parse("#Ideia reunião #ideia com o time #Trabalho")
        assertEquals("reunião com o time", draft.text)
        assertEquals(listOf("ideia", "trabalho"), draft.tags)
    }

    @Test
    fun `no tags leaves the text intact`() {
        val draft = NoteInput.parse("só um lembrete")
        assertEquals("só um lembrete", draft.text)
        assertTrue(draft.tags.isEmpty())
    }

    @Test
    fun `a lone hash is treated as text`() {
        val draft = NoteInput.parse("preço # 10")
        assertEquals("preço # 10", draft.text)
        assertTrue(draft.tags.isEmpty())
    }

    @Test
    fun `only tags yields empty text but a savable draft`() {
        val draft = NoteInput.parse("#a #b")
        assertEquals("", draft.text)
        assertEquals(listOf("a", "b"), draft.tags)
        assertTrue(!draft.isBlank) // tem tags → salvável
    }

    @Test
    fun `blank input is a blank draft`() {
        assertTrue(NoteInput.parse("   ").isBlank)
    }
}
