package com.memora.feature.digest

import com.memora.core.common.time.DayRange
import com.memora.core.digest.DigestSource

/**
 * Fonte das linhas do dia que alimentam o digest. Interface no feature; a implementação real (sobre
 * o `SegmentDao`) fica em `:app` — assim `:core:db` não vira dependência da UI e o ViewModel roda
 * com um fake. Retorna um instantâneo (não um `Flow`): o digest é gerado sob demanda, não observado.
 */
interface DigestSources {
    suspend fun forDay(range: DayRange): List<DigestSource>
}
