package com.memora.feature.today

import com.memora.core.common.model.SpeakerLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class TodayTimelineTest {

    private fun utterance(id: String, startMs: Long, endMs: Long = startMs + 10) =
        TodayItem.Utterance(id = id, text = "t-$id", startMs = startMs, endMs = endMs, speaker = SpeakerLabel.UNKNOWN)

    private fun gap(fromMs: Long, toMs: Long = fromMs + 10, reason: TodayGapReason = TodayGapReason.AUDIO_MISSING) =
        TodayItem.Gap(fromMs = fromMs, toMs = toMs, reason = reason)

    @Test
    fun `merge orders utterances and gaps chronologically`() {
        val result = TodayTimeline.merge(
            utterances = listOf(utterance("a", 300), utterance("b", 100)),
            gaps = listOf(gap(200)),
        )

        assertEquals(listOf(100L, 200L, 300L), result.map { it.atMs })
        assertEquals(listOf("b", null, "a"), result.map { (it as? TodayItem.Utterance)?.id })
    }

    @Test
    fun `at equal start an utterance comes before a gap`() {
        val result = TodayTimeline.merge(
            utterances = listOf(utterance("u", 500)),
            gaps = listOf(gap(500)),
        )

        assertEquals(TodayItem.Utterance::class, result[0]::class)
        assertEquals(TodayItem.Gap::class, result[1]::class)
    }

    @Test
    fun `ordering is independent of input order`() {
        val u = listOf(utterance("z", 100), utterance("a", 100, endMs = 105))
        val forward = TodayTimeline.merge(u, emptyList())
        val reversed = TodayTimeline.merge(u.reversed(), emptyList())

        assertEquals(forward, reversed)
        // shorter (earlier endMs) first at equal start
        assertEquals("a", (forward[0] as TodayItem.Utterance).id)
    }

    @Test
    fun `empty sources yield empty timeline`() {
        assertEquals(emptyList<TodayItem>(), TodayTimeline.merge(emptyList(), emptyList()))
    }
}
