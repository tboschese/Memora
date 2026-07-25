package com.memora.core.common.timeline

import org.junit.Assert.assertEquals
import org.junit.Test

class DayTimelineTest {

    private fun speech(id: String, at: Long) = DayItem.Speech(id, at, "fala-$id")
    private fun note(id: String, at: Long) = DayItem.UserNote(id, at, "nota-$id")
    private fun gap(at: Long, to: Long) = DayItem.Gap(at, to, "AUDIO_MISSING")

    @Test
    fun `merges the three sources chronologically`() {
        val result = DayTimeline.merge(
            listOf(speech("s", 300)),
            listOf(note("n", 100)),
            listOf(gap(200, 210)),
        )
        assertEquals(listOf(100L, 200L, 300L), result.map { it.atMs })
        assertEquals(DayItem.UserNote::class, result[0]::class)
        assertEquals(DayItem.Gap::class, result[1]::class)
        assertEquals(DayItem.Speech::class, result[2]::class)
    }

    @Test
    fun `at equal time speech precedes note precedes gap`() {
        val result = DayTimeline.merge(
            listOf(gap(500, 510)),
            listOf(note("n", 500)),
            listOf(speech("s", 500)),
        )
        assertEquals(
            listOf(DayItem.Speech::class, DayItem.UserNote::class, DayItem.Gap::class),
            result.map { it::class },
        )
    }

    @Test
    fun `ordering is independent of argument order`() {
        val a = DayTimeline.merge(listOf(speech("a", 10)), listOf(note("b", 5)))
        val b = DayTimeline.merge(listOf(note("b", 5)), listOf(speech("a", 10)))
        assertEquals(a, b)
    }

    @Test
    fun `no sources yields empty timeline`() {
        assertEquals(emptyList<DayItem>(), DayTimeline.merge())
    }
}
