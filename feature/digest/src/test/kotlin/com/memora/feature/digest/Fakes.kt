package com.memora.feature.digest

import com.memora.core.common.time.DayRange
import com.memora.core.digest.DigestSource

/** Fonte fake: devolve uma lista fixa (ou vazia), sem banco. */
class FakeDigestSources(private val result: List<DigestSource> = emptyList()) : DigestSources {
    override suspend fun forDay(range: DayRange): List<DigestSource> = result
}

/** Fonte que sempre falha — para exercitar o caminho de erro do ViewModel. */
class FailingDigestSources : DigestSources {
    override suspend fun forDay(range: DayRange): List<DigestSource> = error("fonte indisponível")
}
