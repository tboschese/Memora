package com.memora.app.data

import com.memora.feature.notes.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class HistoryMarkdownTest {

    private val zone = ZoneOffset.UTC
    private fun at(date: LocalDate, h: Int, m: Int) =
        date.atTime(h, m).toInstant(ZoneOffset.UTC).toEpochMilli()

    private val day1 = LocalDate.of(2026, 7, 24)
    private val day2 = LocalDate.of(2026, 7, 25)

    @Test
    fun `empty history has a friendly message`() {
        assertTrue(exportHistoryMarkdown(emptyList(), zone).contains("Sem anotações"))
    }

    @Test
    fun `groups by day in chronological order`() {
        val md = exportHistoryMarkdown(
            listOf(
                Note("b", "de hoje", at(day2, 9, 0)),
                Note("a", "de ontem", at(day1, 8, 0)),
            ),
            zone,
        )
        assertTrue(md.indexOf("## 2026-07-24") < md.indexOf("## 2026-07-25")) // dia 24 antes do 25
        assertTrue(md.contains("- **08:00** de ontem"))
        assertTrue(md.contains("- **09:00** de hoje"))
    }

    @Test
    fun `done tasks are struck through and tags shown`() {
        val md = exportHistoryMarkdown(
            listOf(Note("t", "comprar pão", at(day1, 7, 0), tags = listOf("tarefa"), done = true)),
            zone,
        )
        assertTrue(md.contains("- **07:00** ~~comprar pão~~ #tarefa"))
    }

    @Test
    fun `notes within a day are ordered by time`() {
        val md = exportHistoryMarkdown(
            listOf(
                Note("late", "tarde", at(day1, 15, 0)),
                Note("early", "cedo", at(day1, 6, 0)),
            ),
            zone,
        )
        assertTrue(md.indexOf("cedo") < md.indexOf("tarde"))
    }

    @Test
    fun `export then import round-trips text, tags, done and time`() {
        val original = listOf(
            Note("a", "comprar pão", at(day1, 8, 30), tags = listOf("tarefa"), done = true),
            Note("b", "ideia legal", at(day2, 14, 0), tags = listOf("ideia")),
            Note("c", "sem tags", at(day2, 15, 0)),
        )
        val md = exportHistoryMarkdown(original, zone)

        var n = 0
        val parsed = parseHistoryMarkdown(md, zone) { "id${n++}" }
            .sortedBy { it.createdAtMs }

        assertEquals(original.map { Triple(it.text, it.tags, it.done) to it.createdAtMs },
            parsed.map { Triple(it.text, it.tags, it.done) to it.createdAtMs })
    }

    @Test
    fun `garbage lines are ignored`() {
        val md = "# título\n\nlixo\n## 2026-07-24\n- linha errada\n- **08:00** válida\n"
        val parsed = parseHistoryMarkdown(md, zone) { "id" }
        assertEquals(listOf("válida"), parsed.map { it.text })
    }
}
