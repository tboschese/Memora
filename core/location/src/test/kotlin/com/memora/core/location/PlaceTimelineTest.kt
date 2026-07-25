package com.memora.core.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class PlaceTimelineTest {

    private fun s(timeMs: Long, place: String?) = PlaceSample(timeMs, place)

    @Test
    fun `empty samples yield empty timeline`() {
        assertEquals(emptyList<PlaceInterval>(), PlaceTimeline.build(emptyList()))
    }

    @Test
    fun `a single sample is one open interval`() {
        val timeline = PlaceTimeline.build(listOf(s(100, "Casa")))
        assertEquals(listOf(PlaceInterval(100, null, "Casa")), timeline)
    }

    @Test
    fun `consecutive same place collapse into one interval`() {
        val timeline = PlaceTimeline.build(listOf(s(0, "Casa"), s(10, "Casa"), s(20, "Casa")))
        assertEquals(listOf(PlaceInterval(0, null, "Casa")), timeline)
    }

    @Test
    fun `a change splits into two intervals at the new place's first sample`() {
        val timeline = PlaceTimeline.build(listOf(s(0, "Casa"), s(30, "Trabalho")))
        assertEquals(
            listOf(
                PlaceInterval(0, 30, "Casa"),
                PlaceInterval(30, null, "Trabalho"),
            ),
            timeline,
        )
    }

    @Test
    fun `hysteresis ignores a lone spurious sample`() {
        // minConfirmations=2: um único "Rua" no meio de "Casa" não troca.
        val timeline = PlaceTimeline.build(
            listOf(s(0, "Casa"), s(10, "Rua"), s(20, "Casa")),
            minConfirmations = 2,
        )
        assertEquals(listOf(PlaceInterval(0, null, "Casa")), timeline)
    }

    @Test
    fun `hysteresis commits after enough consecutive confirmations`() {
        // Duas amostras de "Trabalho" confirmam; o intervalo novo começa na primeira delas (10).
        val timeline = PlaceTimeline.build(
            listOf(s(0, "Casa"), s(10, "Trabalho"), s(20, "Trabalho")),
            minConfirmations = 2,
        )
        assertEquals(
            listOf(
                PlaceInterval(0, 10, "Casa"),
                PlaceInterval(10, null, "Trabalho"),
            ),
            timeline,
        )
    }

    @Test
    fun `null place is a place of its own`() {
        val timeline = PlaceTimeline.build(listOf(s(0, "Casa"), s(10, null)))
        assertEquals(
            listOf(
                PlaceInterval(0, 10, "Casa"),
                PlaceInterval(10, null, null),
            ),
            timeline,
        )
    }

    @Test
    fun `unsorted samples are handled`() {
        val timeline = PlaceTimeline.build(listOf(s(30, "Trabalho"), s(0, "Casa")))
        assertEquals(
            listOf(
                PlaceInterval(0, 30, "Casa"),
                PlaceInterval(30, null, "Trabalho"),
            ),
            timeline,
        )
    }

    @Test
    fun `placeAt resolves the place ruling at an instant`() {
        val timeline = PlaceTimeline.build(listOf(s(0, "Casa"), s(100, "Trabalho")))
        assertEquals("Casa", PlaceTimeline.placeAt(timeline, 0))
        assertEquals("Casa", PlaceTimeline.placeAt(timeline, 99))
        assertEquals("Trabalho", PlaceTimeline.placeAt(timeline, 100))
        assertEquals("Trabalho", PlaceTimeline.placeAt(timeline, 5_000))
    }

    @Test
    fun `placeAt is null before the first sample`() {
        val timeline = PlaceTimeline.build(listOf(s(100, "Casa")))
        assertNull(PlaceTimeline.placeAt(timeline, 50))
    }

    @Test
    fun `minConfirmations below one is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            PlaceTimeline.build(listOf(s(0, "Casa")), minConfirmations = 0)
        }
    }
}
