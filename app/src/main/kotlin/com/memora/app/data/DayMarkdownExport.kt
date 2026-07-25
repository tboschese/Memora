package com.memora.app.data

import com.memora.core.common.export.DayEntry
import com.memora.core.common.export.DayEntryKind
import com.memora.core.common.export.DayExport
import com.memora.core.common.export.MarkdownExporter
import com.memora.core.common.timeline.DayItem
import java.time.LocalDate
import java.time.ZoneId

/**
 * Liga a timeline unificada ([DayItem]) ao [MarkdownExporter]: converte os itens do dia em entradas
 * exportáveis e gera o Markdown (RF-25). Os gaps não vão para o export — são rastro interno da
 * timeline, não conteúdo do dia. Fica em `:app`, onde as duas peças de `:core:common` se encontram
 * com os dados reais.
 */
fun List<DayItem>.toDayEntries(): List<DayEntry> = mapNotNull { item ->
    when (item) {
        is DayItem.Speech -> DayEntry(item.atMs, DayEntryKind.SPEECH, item.text, speaker = item.speaker)
        is DayItem.UserNote -> DayEntry(item.atMs, DayEntryKind.NOTE, item.text, tags = item.tags)
        is DayItem.Gap -> null
    }
}

/** Gera o Markdown do dia a partir dos itens da timeline unificada. */
fun exportDayMarkdown(
    items: List<DayItem>,
    date: LocalDate,
    places: List<String>,
    zone: ZoneId,
): String = MarkdownExporter.export(DayExport(date, items.toDayEntries(), places), zone)
