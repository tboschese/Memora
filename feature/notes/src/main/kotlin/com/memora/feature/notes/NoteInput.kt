package com.memora.feature.notes

import java.util.Locale

/**
 * Interpreta o texto cru digitado numa anotação, separando as tags rápidas (`#tag`) do corpo (RF-08).
 * Puro e testável. As tags saem normalizadas (sem `#`, minúsculas, sem repetir); um `#` solto é texto.
 */
object NoteInput {

    fun parse(raw: String): NoteDraft {
        val tokens = raw.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val isTag = { token: String -> token.length > 1 && token.startsWith("#") }

        val tags = tokens.filter(isTag)
            .map { it.removePrefix("#").lowercase(Locale.ROOT) }
            .distinct()
        val text = tokens.filterNot(isTag).joinToString(" ")

        return NoteDraft(text = text, tags = tags)
    }
}
