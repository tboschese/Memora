package com.memora.feature.onboarding

/**
 * Política de rate-limiting do desbloqueio (pura). Um PIN de 4–8 dígitos tem baixa entropia, então
 * após algumas tentativas erradas impomos uma espera com backoff exponencial — encarece um ataque
 * de força bruta sem punir o erro ocasional. Sem estado nem relógio: só o mapa nº de falhas → espera.
 */
object PinLockout {
    /** Tentativas livres (sem espera). */
    const val FREE_ATTEMPTS = 3
    const val BASE_DELAY_MS = 30_000L // 30s na primeira espera
    const val MAX_DELAY_MS = 300_000L // teto de 5 min

    /** Espera imposta **após** [failures] falhas consecutivas (0 enquanto dentro das livres). */
    fun delayMsAfter(failures: Int): Long {
        if (failures <= FREE_ATTEMPTS) return 0
        val over = (failures - FREE_ATTEMPTS).coerceAtMost(20) // cap evita overflow do shift
        return (BASE_DELAY_MS shl (over - 1)).coerceAtMost(MAX_DELAY_MS)
    }
}
