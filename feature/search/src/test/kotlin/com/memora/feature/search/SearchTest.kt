package com.memora.feature.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchQueryParserTest {

    @Test
    fun `splits free terms, tags and speaker`() {
        val q = SearchQueryParser.parse("reunião orçamento #trabalho @self")
        assertEquals(listOf("reunião", "orçamento"), q.terms)
        assertEquals(listOf("trabalho"), q.tags)
        assertEquals("self", q.speaker)
    }

    @Test
    fun `normalizes and deduplicates`() {
        val q = SearchQueryParser.parse("  Café   café  #Ideia #ideia ")
        assertEquals(listOf("café"), q.terms)
        assertEquals(listOf("ideia"), q.tags)
    }

    @Test
    fun `lone markers are ignored`() {
        val q = SearchQueryParser.parse("# @ texto")
        assertEquals(listOf("texto"), q.terms)
        assertTrue(q.tags.isEmpty())
    }

    @Test
    fun `empty input is an empty query`() {
        assertTrue(SearchQueryParser.parse("   ").isEmpty)
    }

    @Test
    fun `last speaker wins`() {
        assertEquals("other", SearchQueryParser.parse("@self @other").speaker)
    }
}

class SearchMatcherTest {

    private val docs = listOf(
        SearchDocument("1", "Reunião de orçamento", timeMs = 100, tags = listOf("trabalho"), speaker = "SELF"),
        SearchDocument("2", "Ideia para o app", timeMs = 300, tags = listOf("ideia"), speaker = "SELF"),
        SearchDocument("3", "Conversa sobre orçamento", timeMs = 200, tags = listOf("trabalho"), speaker = "OTHER"),
    )

    @Test
    fun `all terms must match, most recent first`() {
        val result = SearchMatcher.match(docs, SearchQueryParser.parse("orçamento"))
        assertEquals(listOf("3", "1"), result.map { it.id }) // 200 antes de 100 (desc)
    }

    @Test
    fun `tag filter narrows results`() {
        val result = SearchMatcher.match(docs, SearchQueryParser.parse("#ideia"))
        assertEquals(listOf("2"), result.map { it.id })
    }

    @Test
    fun `speaker filter narrows results`() {
        val result = SearchMatcher.match(docs, SearchQueryParser.parse("orçamento @other"))
        assertEquals(listOf("3"), result.map { it.id })
    }

    @Test
    fun `combined filters must all hold`() {
        val result = SearchMatcher.match(docs, SearchQueryParser.parse("orçamento #trabalho @self"))
        assertEquals(listOf("1"), result.map { it.id })
    }

    @Test
    fun `an empty query matches nothing`() {
        assertTrue(SearchMatcher.match(docs, SearchQueryParser.parse("")).isEmpty())
    }

    @Test
    fun `term matching is case-insensitive substring`() {
        val result = SearchMatcher.match(docs, SearchQueryParser.parse("REUNI"))
        assertEquals(listOf("1"), result.map { it.id })
    }
}
