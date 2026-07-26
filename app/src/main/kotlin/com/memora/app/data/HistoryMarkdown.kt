package com.memora.app.data

import com.memora.feature.notes.Note
import com.memora.feature.notes.NoteInput
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TIME = DateTimeFormatter.ofPattern("HH:mm")
private val DAY_HEADING = Regex("""^##\s+(\d{4}-\d{2}-\d{2})\s*$""")
private val NOTE_LINE = Regex("""^-\s+\*\*(\d{2}:\d{2})\*\*\s+(.*)$""")

/**
 * Exporta **todo o histórico** de anotações num único Markdown (backup/migração), agrupado por dia em
 * ordem cronológica. Tarefas concluídas saem riscadas; tags acompanham cada linha. Puro e
 * determinístico (fuso por parâmetro).
 */
fun exportHistoryMarkdown(notes: List<Note>, zone: ZoneId): String {
    if (notes.isEmpty()) return "# Memora\n\nSem anotações ainda.\n"

    val sb = StringBuilder("# Memora — histórico\n\n")
    notes.groupBy { Instant.ofEpochMilli(it.createdAtMs).atZone(zone).toLocalDate() }
        .toSortedMap()
        .forEach { (day, dayNotes) ->
            sb.append("## ").append(day).append("\n\n")
            dayNotes.sortedBy { it.createdAtMs }.forEach { note ->
                val time = TIME.format(Instant.ofEpochMilli(note.createdAtMs).atZone(zone))
                sb.append("- **").append(time).append("** ")
                if (note.done) sb.append("~~").append(note.text).append("~~") else sb.append(note.text)
                if (note.tags.isNotEmpty()) {
                    sb.append(' ').append(note.tags.joinToString(" ") { "#$it" })
                }
                sb.append('\n')
            }
            sb.append('\n')
        }
    return sb.toString()
}

/**
 * Parseia o Markdown gerado por [exportHistoryMarkdown] de volta em notas (import/restore). Faz
 * round-trip com o export: texto, tags, conclusão e o instante (data + hora no [zone]) são
 * preservados; ids são novos ([newId]). Linhas que não casam o formato são ignoradas — um import
 * tolerante nunca derruba tudo por causa de uma linha estranha.
 */
fun parseHistoryMarkdown(md: String, zone: ZoneId, newId: () -> String): List<Note> {
    val notes = mutableListOf<Note>()
    var day: LocalDate? = null

    for (raw in md.lines()) {
        val line = raw.trim()
        val heading = DAY_HEADING.matchEntire(line)
        if (heading != null) {
            day = runCatching { LocalDate.parse(heading.groupValues[1]) }.getOrNull()
            continue
        }
        val match = NOTE_LINE.matchEntire(line) ?: continue
        val date = day ?: continue
        val time = runCatching { LocalTime.parse(match.groupValues[1]) }.getOrNull() ?: continue

        val draft = NoteInput.parse(match.groupValues[2])
        val done = draft.text.startsWith("~~") && draft.text.endsWith("~~") && draft.text.length >= 4
        val text = if (done) draft.text.removeSurrounding("~~").trim() else draft.text
        if (text.isEmpty() && draft.tags.isEmpty()) continue

        notes += Note(
            id = newId(),
            text = text,
            createdAtMs = date.atTime(time).atZone(zone).toInstant().toEpochMilli(),
            tags = draft.tags,
            done = done,
        )
    }
    return notes
}
