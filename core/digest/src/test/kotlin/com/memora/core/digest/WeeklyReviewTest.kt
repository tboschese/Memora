package com.memora.core.digest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyReviewTest {

    private fun digest(
        day: Long,
        themes: List<String> = emptyList(),
        decisions: List<String> = emptyList(),
        actions: List<String> = emptyList(),
    ) = Digest(epochDay = day, summary = "d$day", decisions = decisions, myActionItems = actions, themes = themes)

    @Test
    fun `empty input yields an empty week`() {
        val week = WeeklyReview.aggregate(emptyList())
        assertEquals(0, week.daysCovered)
        assertTrue(week.topThemes.isEmpty())
    }

    @Test
    fun `spans from the earliest to the latest day`() {
        val week = WeeklyReview.aggregate(listOf(digest(12), digest(10), digest(14)))
        assertEquals(10L, week.fromEpochDay)
        assertEquals(14L, week.toEpochDay)
        assertEquals(3, week.daysCovered)
    }

    @Test
    fun `top themes are ranked by frequency then alphabetically`() {
        val week = WeeklyReview.aggregate(
            listOf(
                digest(1, themes = listOf("trabalho", "estudo")),
                digest(2, themes = listOf("trabalho", "saúde")),
                digest(3, themes = listOf("trabalho", "estudo")),
            ),
        )
        // trabalho x3, estudo x2, saúde x1
        assertEquals(listOf("trabalho", "estudo", "saúde"), week.topThemes)
    }

    @Test
    fun `ties break alphabetically`() {
        val week = WeeklyReview.aggregate(listOf(digest(1, themes = listOf("zebra", "alfa"))))
        assertEquals(listOf("alfa", "zebra"), week.topThemes)
    }

    @Test
    fun `decisions are chronological and action items deduped`() {
        val week = WeeklyReview.aggregate(
            listOf(
                digest(2, decisions = listOf("B"), actions = listOf("x", "y")),
                digest(1, decisions = listOf("A"), actions = listOf("x")),
            ),
        )
        assertEquals(listOf("A", "B"), week.allDecisions)       // ordenado por dia
        assertEquals(listOf("x", "y"), week.openActionItems)    // "x" não repete
    }

    @Test
    fun `top themes are capped`() {
        val many = (1..20).map { "tema$it" }
        val week = WeeklyReview.aggregate(listOf(digest(1, themes = many)))
        assertEquals(WeeklyReview.MAX_TOP_THEMES, week.topThemes.size)
    }
}
