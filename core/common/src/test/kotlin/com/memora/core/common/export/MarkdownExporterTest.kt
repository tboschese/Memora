package com.memora.core.common.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class MarkdownExporterTest {

    private val zone = ZoneOffset.UTC
    private val date = LocalDate.of(2026, 7, 24)

    /** epoch-millis de [date] às [h]:[m] em UTC. */
    private fun at(h: Int, m: Int): Long =
        date.atTime(h, m).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test
    fun `exports frontmatter and chronological body`() {
        val export = DayExport(
            date = date,
            entries = listOf(
                DayEntry(at(9, 15), DayEntryKind.NOTE, "comprar pão", tags = listOf("tarefa")),
                DayEntry(at(8, 30), DayEntryKind.SPEECH, "Bom dia", speaker = "SELF"),
            ),
            places = listOf("Casa", "Trabalho"),
        )

        val lines = MarkdownExporter.export(export, zone).lines()

        assertEquals("---", lines[0])
        assertEquals("date: 2026-07-24", lines[1])
        assertEquals("places: [Casa, Trabalho]", lines[2])
        assertEquals("tags: [tarefa]", lines[3])
        assertEquals("---", lines[4])
        assertEquals("", lines[5])
        assertEquals("# 2026-07-24", lines[6])
        assertEquals("", lines[7])
        assertEquals("- **08:30** (SELF) Bom dia", lines[8])   // ordenado por hora
        assertEquals("- **09:15** 📝 comprar pão #tarefa", lines[9])
    }

    @Test
    fun `omits places and tags lines when empty`() {
        val export = DayExport(date, listOf(DayEntry(at(10, 0), DayEntryKind.SPEECH, "oi", speaker = "SELF")))
        val md = MarkdownExporter.export(export, zone)
        assertFalse(md.contains("places:"))
        assertFalse(md.contains("tags:"))
    }

    @Test
    fun `UNKNOWN speaker is not attributed`() {
        val export = DayExport(date, listOf(DayEntry(at(10, 0), DayEntryKind.SPEECH, "algo", speaker = "UNKNOWN")))
        val md = MarkdownExporter.export(export, zone)
        assertTrue(md.contains("- **10:00** algo"))
        assertFalse(md.contains("(UNKNOWN)"))
    }

    @Test
    fun `aggregates and sorts all tags in the frontmatter`() {
        val export = DayExport(
            date,
            listOf(
                DayEntry(at(9, 0), DayEntryKind.NOTE, "a", tags = listOf("tarefa", "ideia")),
                DayEntry(at(10, 0), DayEntryKind.NOTE, "b", tags = listOf("ideia", "pessoal")),
            ),
        )
        val md = MarkdownExporter.export(export, zone)
        assertTrue(md.contains("tags: [ideia, pessoal, tarefa]")) // dedup + ordenado
    }

    @Test
    fun `timezone shifts the displayed time`() {
        val export = DayExport(date, listOf(DayEntry(at(0, 0), DayEntryKind.SPEECH, "meia-noite UTC", speaker = "SELF")))
        // UTC-3: 00:00Z é 21:00 do dia anterior
        val md = MarkdownExporter.export(export, ZoneOffset.ofHours(-3))
        assertTrue(md.contains("- **21:00** (SELF) meia-noite UTC"))
    }
}
