package com.memora.core.digest

import com.memora.core.common.model.SpeakerLabel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeuristicDigestProviderTest {

    private val provider = HeuristicDigestProvider()

    private fun note(text: String, tags: List<String> = emptyList(), place: String? = null, done: Boolean = false) =
        DigestSource(timeMs = 0, speaker = SpeakerLabel.SELF, text = text, place = place, tags = tags, done = done)

    @Test
    fun `completed tasks are not action items`() = runTest {
        val digest = provider.generate(
            DigestInput(
                epochDay = 1,
                sources = listOf(
                    note("comprar pão", tags = listOf("tarefa"), done = true),
                    note("ligar dentista", tags = listOf("tarefa")),
                ),
            ),
        )
        assertEquals(listOf("ligar dentista"), digest.myActionItems)
    }

    @Test
    fun `empty day summarizes as no activity`() = runTest {
        val digest = provider.generate(DigestInput(epochDay = 1, sources = emptyList()))
        assertEquals("Sem atividade registrada.", digest.summary)
        assertTrue(digest.myActionItems.isEmpty())
    }

    @Test
    fun `task-tagged notes become action items`() = runTest {
        val digest = provider.generate(
            DigestInput(
                epochDay = 1,
                sources = listOf(
                    note("comprar pão", tags = listOf("tarefa")),
                    note("ligar pro dentista", tags = listOf("tarefa")),
                    note("dia bonito"),
                ),
            ),
        )
        assertEquals(listOf("comprar pão", "ligar pro dentista"), digest.myActionItems)
    }

    @Test
    fun `decision-tagged notes become decisions (accent-tolerant)`() = runTest {
        val digest = provider.generate(
            DigestInput(
                epochDay = 1,
                sources = listOf(note("adotar Kotlin", tags = listOf("decisao"))),
            ),
        )
        assertEquals(listOf("adotar Kotlin"), digest.decisions)
    }

    @Test
    fun `other tags and glossary terms become themes`() = runTest {
        val digest = provider.generate(
            DigestInput(
                epochDay = 1,
                sources = listOf(
                    note("brainstorm", tags = listOf("ideia", "tarefa")), // tarefa é ação, não tema
                    note("treino", tags = listOf("saúde")),
                ),
                glossaryTerms = listOf("Memora"),
            ),
        )
        assertEquals(listOf("ideia", "saúde", "Memora"), digest.themes)
    }

    @Test
    fun `summary counts records and lists places`() = runTest {
        val digest = provider.generate(
            DigestInput(
                epochDay = 1,
                sources = listOf(
                    note("a", place = "Casa"),
                    note("b", place = "Trabalho"),
                    note("c", place = "Casa"),
                ),
            ),
        )
        assertEquals("3 registros no dia — Casa, Trabalho.", digest.summary)
    }

    @Test
    fun `a single record uses singular`() = runTest {
        val digest = provider.generate(DigestInput(epochDay = 1, sources = listOf(note("só uma"))))
        assertEquals("1 registro no dia.", digest.summary)
    }
}
