package com.memora.core.digest

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class DigestSchedulerTest {

    private val zone: ZoneId = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 7, 24)
    private val todayEpochDay = today.toEpochDay()

    /** epoch-millis de hoje no horário [hour]:[minute] em UTC. */
    private fun at(hour: Int, minute: Int = 0): Long =
        today.atTime(LocalTime.of(hour, minute)).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun should(nowMs: Long, charging: Boolean, last: Long? = null): Boolean =
        DigestScheduler.shouldGenerate(nowMs, zone, charging, last)

    @Test
    fun `before target hour it waits even while charging`() {
        assertFalse(should(at(20, 59), charging = true))
    }

    @Test
    fun `at target hour it generates when charging`() {
        assertTrue(should(at(21, 0), charging = true))
    }

    @Test
    fun `between target and deadline it waits when not charging`() {
        assertFalse(should(at(22, 0), charging = false))
    }

    @Test
    fun `after deadline it generates even without charging`() {
        assertTrue(should(at(23, 0), charging = false))
    }

    @Test
    fun `it does not generate twice on the same day`() {
        assertFalse(should(at(23, 30), charging = true, last = todayEpochDay))
    }

    @Test
    fun `a day not yet generated is eligible`() {
        // último digest foi ontem; hoje 21h carregando → gera.
        assertTrue(should(at(21, 0), charging = true, last = todayEpochDay - 1))
    }

    @Test
    fun `early morning of a new day still waits until the target hour`() {
        assertFalse(should(at(1, 0), charging = true, last = todayEpochDay - 1))
    }
}
