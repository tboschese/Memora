package com.memora.core.glossary

import java.util.Locale

/**
 * Aprende correções do glossário a partir de edições manuais (RF-33): quando o usuário troca uma
 * grafia errada pela certa, "adicionar como correção automática" registra a variante na entrada
 * canônica, para o [GlossaryCorrector] passar a corrigi-la sozinho dali em diante.
 *
 * Puro: recebe o glossário atual e devolve o novo, sem tocar em persistência (o adaptador salva).
 */
object GlossaryEditor {

    /**
     * Registra que [variant] deve virar [canonical]. Se já existe uma entrada com essa grafia
     * canônica (case-insensitive), acrescenta a variante (sem duplicar); senão cria uma entrada nova
     * com [newId]. Variante vazia, ou igual à canônica, é no-op (não há o que corrigir).
     */
    fun learnCorrection(
        entries: List<GlossaryEntry>,
        variant: String,
        canonical: String,
        newId: () -> String,
    ): List<GlossaryEntry> {
        val cleanVariant = variant.trim()
        val cleanCanonical = canonical.trim()
        if (cleanVariant.isEmpty() || cleanCanonical.isEmpty()) return entries
        if (cleanVariant.equalsIgnoreCase(cleanCanonical)) return entries

        val index = entries.indexOfFirst { it.canonical.equalsIgnoreCase(cleanCanonical) }
        if (index < 0) {
            return entries + GlossaryEntry(id = newId(), canonical = cleanCanonical, variants = listOf(cleanVariant))
        }
        val entry = entries[index]
        if (entry.variants.any { it.equalsIgnoreCase(cleanVariant) }) return entries
        val updated = entry.copy(variants = entry.variants + cleanVariant)
        return entries.toMutableList().also { it[index] = updated }
    }

    private fun String.equalsIgnoreCase(other: String): Boolean =
        lowercase(Locale.ROOT) == other.lowercase(Locale.ROOT)
}
