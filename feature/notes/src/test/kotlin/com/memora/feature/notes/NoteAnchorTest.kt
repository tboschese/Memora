package com.memora.feature.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteAnchorTest {

    private val segments = listOf(
        AnchorableSegment("a", startMs = 0, endMs = 100),
        AnchorableSegment("b", startMs = 200, endMs = 300),
    )

    @Test
    fun `anchors to the segment whose interval contains the instant`() {
        assertEquals("a", NoteAnchor.anchorFor(50, segments))
        assertEquals("b", NoteAnchor.anchorFor(250, segments))
    }

    @Test
    fun `boundaries are inclusive`() {
        assertEquals("a", NoteAnchor.anchorFor(0, segments))
        assertEquals("a", NoteAnchor.anchorFor(100, segments))
    }

    @Test
    fun `between segments anchors to the most recent one already started`() {
        // 150 está no silêncio entre "a" (0..100) e "b" (200..300) → ancora em "a".
        assertEquals("a", NoteAnchor.anchorFor(150, segments))
    }

    @Test
    fun `after the last segment anchors to it`() {
        assertEquals("b", NoteAnchor.anchorFor(5_000, segments))
    }

    @Test
    fun `before any segment is a standalone note`() {
        assertNull(NoteAnchor.anchorFor(-1, segments))
    }

    @Test
    fun `no segments is a standalone note`() {
        assertNull(NoteAnchor.anchorFor(100, emptyList()))
    }
}
