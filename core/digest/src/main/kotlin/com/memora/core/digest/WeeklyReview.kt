package com.memora.core.digest

/**
 * Revisão semanal (Fase 4, §7): cruza os digests de vários dias numa visão única. [daysCovered] é
 * quantos dias distintos entraram; [topThemes] são os temas mais recorrentes (frequência desc, e
 * alfabético no empate); [allDecisions] são todas as decisões em ordem cronológica; [openActionItems]
 * junta os action items sem repetir. Vazio quando não há digests.
 */
data class WeeklyDigest(
    val fromEpochDay: Long,
    val toEpochDay: Long,
    val daysCovered: Int,
    val topThemes: List<String>,
    val allDecisions: List<String>,
    val openActionItems: List<String>,
)

/** Agrega uma lista de [Digest] diários num [WeeklyDigest]. Puro e determinístico. */
object WeeklyReview {

    /** Quantos temas manter no destaque semanal. */
    const val MAX_TOP_THEMES = 10

    fun aggregate(digests: List<Digest>): WeeklyDigest {
        if (digests.isEmpty()) {
            return WeeklyDigest(0, 0, 0, emptyList(), emptyList(), emptyList())
        }
        val ordered = digests.sortedBy { it.epochDay }

        val themeCounts = LinkedHashMap<String, Int>()
        for (theme in ordered.flatMap { it.themes }) {
            themeCounts[theme] = (themeCounts[theme] ?: 0) + 1
        }
        val topThemes = themeCounts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(MAX_TOP_THEMES)
            .map { it.key }

        return WeeklyDigest(
            fromEpochDay = ordered.first().epochDay,
            toEpochDay = ordered.last().epochDay,
            daysCovered = ordered.map { it.epochDay }.distinct().size,
            topThemes = topThemes,
            allDecisions = ordered.flatMap { it.decisions },
            openActionItems = ordered.flatMap { it.myActionItems }.distinct(),
        )
    }
}
