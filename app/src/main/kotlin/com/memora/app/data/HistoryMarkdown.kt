package com.memora.app.data

import com.memora.feature.notes.Note
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TIME = DateTimeFormatter.ofPattern("HH:mm")

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
