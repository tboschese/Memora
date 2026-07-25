package com.memora.core.common.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class DayRangeTest {

    private val saoPaulo = ZoneId.of("America/Sao_Paulo") // UTC-3, sem horário de verão hoje

    @Test
    fun `of spans local midnight to next midnight`() {
        val range = DayRange.of(LocalDate.of(2026, 7, 21), saoPaulo)

        // 2026-07-21 00:00 em UTC-3 = 03:00Z
        assertEquals(Instant.parse("2026-07-21T03:00:00Z").toEpochMilli(), range.fromMs)
        assertEquals(Instant.parse("2026-07-22T03:00:00Z").toEpochMilli(), range.toMs)
    }

    @Test
    fun `containing picks the civil day of the instant in the given zone`() {
        // 02:00Z de 22/07 ainda é 23:00 de 21/07 em UTC-3
        val instant = Instant.parse("2026-07-22T02:00:00Z")
        val range = DayRange.containing(instant, saoPaulo)

        assertEquals(DayRange.of(LocalDate.of(2026, 7, 21), saoPaulo), range)
    }

    @Test
    fun `zone changes the boundaries`() {
        val date = LocalDate.of(2026, 7, 21)
        val utc = DayRange.of(date, ZoneOffset.UTC)

        assertEquals(Instant.parse("2026-07-21T00:00:00Z").toEpochMilli(), utc.fromMs)
        assertEquals(Instant.parse("2026-07-22T00:00:00Z").toEpochMilli(), utc.toMs)
    }

    @Test
    fun `range is exactly 24h on a regular day`() {
        val range = DayRange.of(LocalDate.of(2026, 7, 21), saoPaulo)
        assertEquals(24 * 60 * 60 * 1000L, range.toMs - range.fromMs)
    }

    @Test
    fun `rejects inverted bounds`() {
        assertThrows(IllegalArgumentException::class.java) { DayRange(10, 5) }
    }
}
