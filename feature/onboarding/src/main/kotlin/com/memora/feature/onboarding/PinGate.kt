package com.memora.feature.onboarding

/**
 * Porta de acesso do fluxo de PIN. A UI só vê resultados — nunca a chave derivada: a implementação
 * real (em `:app`) deriva a chave via `PinVault`, abre o banco cifrado e destranca o auto-lock, e
 * zera o material sensível. Assim os ViewModels são testáveis com um fake e a chave não vaza para
 * a camada de UI. Princípio 4: destrancar a leitura não interfere na captura.
 */
interface PinGate {
    /** Já existe um PIN configurado? Decide onboarding (setup) vs. desbloqueio. */
    fun isConfigured(): Boolean

    /** Primeira configuração: cria o PIN e abre a sessão. Não falha por PIN "errado" (é o primeiro). */
    suspend fun setup(pin: CharArray)

    /** Desbloqueio: `true` se o PIN confere (sessão aberta); `false` se está errado. */
    suspend fun unlock(pin: CharArray): Boolean
}
