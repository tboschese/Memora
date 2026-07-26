package com.memora.core.digest

/**
 * Digest **sem LLM**: deriva um resumo estruturado do dia por heurística sobre as anotações e suas
 * tags (RF-08). Útil de verdade enquanto o modelo local não existe, e determinístico — o mesmo dia
 * gera sempre o mesmo digest.
 *
 * - itens de ação: fontes marcadas `#tarefa`;
 * - decisões: fontes marcadas `#decisão`/`#decisao`;
 * - temas: as demais tags do dia + as grafias do glossário;
 * - resumo: contagem de registros e lugares visitados.
 */
class HeuristicDigestProvider : DigestProvider {

    override suspend fun generate(input: DigestInput): Digest {
        val sources = input.sources
        if (sources.isEmpty()) {
            return Digest(epochDay = input.epochDay, summary = "Sem atividade registrada.")
        }

        // Tarefas ainda pendentes viram itens de ação; as concluídas saem da lista.
        val actionItems = sources.filterNot { it.done }.textsWithTag(TAG_TASK)
        val decisions = sources.textsWithAnyTag(TAG_DECISION)
        val usedTags = TAG_DECISION + TAG_TASK
        val themes = (sources.flatMap { it.tags }.filter { it !in usedTags } + input.glossaryTerms)
            .distinct()

        val doneTasks = sources.count { it.done && TAG_TASK in it.tags }
        val places = sources.mapNotNull { it.place }.distinct()
        val summary = buildString {
            append(sources.size).append(" registro").append(if (sources.size == 1) "" else "s").append(" no dia")
            if (doneTasks > 0) {
                append(", ").append(doneTasks).append(" tarefa").append(if (doneTasks == 1) "" else "s").append(" concluída")
                if (doneTasks > 1) append('s')
            }
            if (places.isNotEmpty()) append(" — ").append(places.joinToString(", "))
            append('.')
        }

        return Digest(
            epochDay = input.epochDay,
            summary = summary,
            decisions = decisions,
            myActionItems = actionItems,
            themes = themes,
        )
    }

    private fun List<DigestSource>.textsWithTag(tag: String): List<String> =
        filter { tag in it.tags }.map { it.text }.filter { it.isNotBlank() }.distinct()

    private fun List<DigestSource>.textsWithAnyTag(tags: Set<String>): List<String> =
        filter { source -> source.tags.any { it in tags } }.map { it.text }.filter { it.isNotBlank() }.distinct()

    private companion object {
        const val TAG_TASK = "tarefa"
        val TAG_DECISION = setOf("decisão", "decisao")
    }
}
