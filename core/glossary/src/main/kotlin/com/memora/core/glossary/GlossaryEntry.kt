package com.memora.core.glossary

/**
 * Termo do glossário do usuário. Corrige grafias que o Whisper erra (nomes próprios, jargão).
 * Injetado em 3 pontos do pipeline: initial_prompt do Whisper, correção pós-transcrição (aqui)
 * e system prompt do digest.
 */
data class GlossaryEntry(
    val id: String,
    /** Grafia correta/canônica (ex.: "Kubernetes"). */
    val canonical: String,
    /** Variantes erradas que devem ser substituídas (ex.: "kubernetis", "cubernetes"). */
    val variants: List<String> = emptyList(),
    val description: String? = null,
)
