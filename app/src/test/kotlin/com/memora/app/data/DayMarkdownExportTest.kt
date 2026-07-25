package com.memora.app.data

import com.memora.core.common.timeline.DayItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class DayMarkdownExportTest {

    private val zone = ZoneOffset.UTC
    private val date = LocalDate.of(2026, 7, 24)
    private fun at(h: Int, m: Int) = date.atTime(h, m).toInstant(ZoneOffset.UTC).toEpochMilli()

    private val items = listOf(
        DayItem.Speech("s", at(8, 0), "Bom dia", speaker = "SELF"),
        DayItem.UserNote("n", at(9, 0), "comprar pão", tags = listOf("tarefa")),
        DayItem.Gap(at(10, 0), at(10, 5), "AUDIO_MISSING"),
    )

    @Test
    fun `gaps are dropped from the entries`() {
        val entries = items.toDayEntries()
        assertEquals(2, entries.size) // fala + nota, sem o gap
    }

    @Test
    fun `renders speech and notes but not the gap`() {
        val md = exportDayMarkdown(items, date, places = listOf("Casa"), zone = zone)
        assertTrue(md.contains("- **08:00** (SELF) Bom dia"))
        assertTrue(md.contains("- **09:00** 📝 comprar pão #tarefa"))
        assertFalse(md.contains("AUDIO_MISSING"))
        assertTrue(md.contains("places: [Casa]"))
    }

    @Test
    fun `an empty timeline still produces a valid header`() {
        val md = exportDayMarkdown(emptyList(), date, places = emptyList(), zone = zone)
        assertTrue(md.contains("# 2026-07-24"))
    }
}
