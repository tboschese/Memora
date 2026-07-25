package com.memora.feature.notes

/**
 * Anotação do usuário (§5.1). Domínio do feature — o adaptador em `:app` mapeia de/para o
 * `NoteEntity`. [tags] são tokens rápidos (#reunião/#ideia/…) já sem o `#`; a UI decora.
 */
data class Note(
    val id: String,
    val text: String,
    val createdAtMs: Long,
    val tags: List<String> = emptyList(),
    /** Segmento ancorado (timestamp exato), quando a nota nasceu durante a captura. */
    val segmentId: String? = null,
    val place: String? = null,
)

/**
 * Rascunho de uma nota nova, antes de virar [Note] persistida (que ganha id e `createdAtMs`).
 * Um rascunho sem texto nem tags é vazio ([isBlank]) e não deve ser salvo.
 */
data class NoteDraft(
    val text: String,
    val tags: List<String> = emptyList(),
    val segmentId: String? = null,
    val place: String? = null,
) {
    val isBlank: Boolean get() = text.isBlank() && tags.isEmpty()
}
