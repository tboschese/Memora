package com.memora.core.common.export

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Tipo de entrada exportada: uma fala transcrita ou uma anotação do usuário. */
enum class DayEntryKind { SPEECH, NOTE }

/** Uma linha do dia a exportar, posicionada por [timeMs]. */
data class DayEntry(
    val timeMs: Long,
    val kind: DayEntryKind,
    val text: String,
    val speaker: String? = null,
    val tags: List<String> = emptyList(),
)

/** Um dia inteiro pronto para virar Markdown. [places] são os lugares visitados no dia. */
data class DayExport(
    val date: LocalDate,
    val entries: List<DayEntry>,
    val places: List<String> = emptyList(),
)

/**
 * Exporta um dia para Markdown com frontmatter YAML (Obsidian/PARA — RF-25). Puro e determinístico:
 * o fuso entra por parâmetro, então o mesmo dia gera sempre o mesmo texto. As entradas saem em ordem
 * cronológica; falas trazem o speaker (exceto `UNKNOWN`, que não se atribui — regra 5) e notas trazem
 * as tags. O frontmatter agrega `places` e todas as tags do dia, omitindo o que estiver vazio.
 */
object MarkdownExporter {

    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    fun export(export: DayExport, zone: ZoneId): String {
        val sb = StringBuilder()
        val allTags = export.entries.flatMap { it.tags }.distinct().sorted()

        sb.append("---\n")
        sb.append("date: ").append(export.date).append('\n')
        if (export.places.isNotEmpty()) {
            sb.append("places: [").append(export.places.joinToString(", ")).append("]\n")
        }
        if (allTags.isNotEmpty()) {
            sb.append("tags: [").append(allTags.joinToString(", ")).append("]\n")
        }
        sb.append("---\n\n")
        sb.append("# ").append(export.date).append("\n\n")

        for (entry in export.entries.sortedBy { it.timeMs }) {
            val time = timeFormat.format(Instant.ofEpochMilli(entry.timeMs).atZone(zone))
            sb.append("- **").append(time).append("** ")
            when (entry.kind) {
                DayEntryKind.SPEECH -> {
                    val speaker = entry.speaker?.takeIf { it.isNotBlank() && it != "UNKNOWN" }
                    if (speaker != null) sb.append('(').append(speaker).append(") ")
                    sb.append(entry.text.trim())
                }
                DayEntryKind.NOTE -> {
                    sb.append("📝 ").append(entry.text.trim())
                    if (entry.tags.isNotEmpty()) {
                        sb.append(' ').append(entry.tags.joinToString(" ") { "#$it" })
                    }
                }
            }
            sb.append('\n')
        }
        return sb.toString()
    }
}
