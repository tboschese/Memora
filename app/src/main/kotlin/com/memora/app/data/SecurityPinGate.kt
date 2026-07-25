package com.memora.app.data

import com.memora.core.security.AutoLockController
import com.memora.core.security.PinVault
import com.memora.feature.onboarding.PinGate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sessão cifrada aberta com a chave derivada do PIN. É um *seam* de device — a implementação real
 * chama `buildEncryptedDatabase` (SQLCipher nativo, não roda sob Robolectric); um fake basta para
 * testar o gate. [open] recebe a chave e a copia para dentro do SQLCipher; quem chama a zera depois.
 */
interface EncryptedSession {
    fun open(key: ByteArray)
    fun close()
}

/**
 * Liga o fluxo de PIN (`:feature:onboarding`) à segurança real (`:core:security`) e à sessão cifrada.
 * Fica em `:app`, a raiz de composição — assim `:feature:onboarding` não conhece `PinVault` nem a
 * chave. Deriva a chave, abre a sessão e destranca o auto-lock, sempre **zerando** a chave em
 * seguida (regra 3). O PBKDF2 roda fora da main thread ([derivationDispatcher]).
 */
class SecurityPinGate(
    private val vault: PinVault,
    private val autoLock: AutoLockController,
    private val session: EncryptedSession,
    private val clock: () -> Long = System::currentTimeMillis,
    private val derivationDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : PinGate {

    override fun isConfigured(): Boolean = vault.isInitialized()

    override suspend fun setup(pin: CharArray) {
        val key = withContext(derivationDispatcher) { vault.setup(pin) }
        openAndUnlock(key)
    }

    override suspend fun unlock(pin: CharArray): Boolean {
        val key = withContext(derivationDispatcher) { vault.unlock(pin) } ?: return false
        openAndUnlock(key)
        return true
    }

    /** Abre a sessão com a chave e destranca a leitura; zera a chave mesmo se a abertura falhar. */
    private fun openAndUnlock(key: ByteArray) {
        try {
            session.open(key)
        } finally {
            key.fill(0)
        }
        autoLock.unlock(clock())
    }
}
