package com.memora.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.core.glossary.GlossaryEntry
import com.memora.core.glossary.GlossaryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Gerência do glossário: lista os termos e permite adicionar/remover. As variantes são digitadas
 * separadas por vírgula. Os termos alimentam os 3 pontos de injeção quando a transcrição existir.
 */
class GlossaryViewModel(
    private val repository: GlossaryRepository,
    private val newId: () -> String,
) : ViewModel() {

    val entries: StateFlow<List<GlossaryEntry>> =
        repository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Adiciona um termo; [variantsRaw] são variantes separadas por vírgula. Ignora canônica vazia. */
    fun add(canonical: String, variantsRaw: String) {
        val clean = canonical.trim()
        if (clean.isEmpty()) return
        val variants = variantsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        viewModelScope.launch {
            repository.save(GlossaryEntry(id = newId(), canonical = clean, variants = variants))
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }
}
