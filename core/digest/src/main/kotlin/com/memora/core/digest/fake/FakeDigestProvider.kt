package com.memora.core.digest.fake

import com.memora.core.digest.Digest
import com.memora.core.digest.DigestInput
import com.memora.core.digest.DigestProvider

/**
 * Fake do [DigestProvider]. Monta um digest determinístico a partir das fontes (sem LLM),
 * suficiente para testar a UI de Digest e o fluxo de geração. `lastInput` permite asserts.
 */
class FakeDigestProvider : DigestProvider {

    var lastInput: DigestInput? = null
        private set

    override suspend fun generate(input: DigestInput): Digest {
        lastInput = input
        val summary = if (input.sources.isEmpty()) {
            "Sem atividade registrada."
        } else {
            "${input.sources.size} trechos ao longo do dia."
        }
        return Digest(
            epochDay = input.epochDay,
            summary = summary,
            themes = input.glossaryTerms.take(3),
        )
    }
}
