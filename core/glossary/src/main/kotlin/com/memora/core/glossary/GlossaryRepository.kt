package com.memora.core.glossary

import kotlinx.coroutines.flow.Flow

/**
 * Fonte de verdade do glossário do usuário. Interface aqui; a impl real (sobre o Room) fica em
 * `:app`. Alimenta os 3 pontos de injeção — `initial_prompt` do Whisper ([GlossaryPrompt]), correção
 * pós-transcrição ([GlossaryCorrector]) e system prompt do digest — que consomem `observeAll`.
 */
interface GlossaryRepository {
    fun observeAll(): Flow<List<GlossaryEntry>>

    suspend fun save(entry: GlossaryEntry)

    suspend fun delete(id: String)
}
