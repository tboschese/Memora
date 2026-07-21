package com.memora.core.security

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Setup e verificação do PIN + derivação da chave do banco.
 *
 * A chave **nunca é persistida**: é re-derivada do PIN a cada [unlock]. Para verificar o PIN sem
 * guardar a chave, persistimos um `verifier = SHA-256(chave)`; no unlock, re-derivamos e comparamos
 * em tempo constante. Salt e verifier vivem no [SecurityStore] (cifrado pelo Keystore na impl real,
 * o que eleva o custo de um ataque offline sobre PINs de baixa entropia).
 *
 * Lógica pura e testável com o fake em memória — sem Android/Keystore.
 */
class PinVault(
    private val store: SecurityStore,
    private val iterations: Int = KeyDerivation.DEFAULT_ITERATIONS,
    private val random: SecureRandom = SecureRandom(),
) {
    fun isInitialized(): Boolean = store.readSalt() != null && store.readVerifier() != null

    /**
     * Cria o PIN: gera salt, deriva a chave e guarda salt + verifier. Retorna a chave para abrir o
     * banco (o chamador deve zerar o array após uso).
     */
    fun setup(pin: CharArray): ByteArray {
        val salt = ByteArray(KeyDerivation.SALT_BYTES).also(random::nextBytes)
        val key = KeyDerivation.deriveKey(pin, salt, iterations)
        store.writeSalt(salt)
        store.writeVerifier(sha256(key))
        return key
    }

    /**
     * Verifica o PIN e retorna a chave do banco, ou `null` se o PIN estiver errado ou o vault não
     * estiver inicializado. Em caso de PIN errado, a chave derivada é zerada antes de retornar.
     */
    fun unlock(pin: CharArray): ByteArray? {
        val salt = store.readSalt() ?: return null
        val verifier = store.readVerifier() ?: return null
        val key = KeyDerivation.deriveKey(pin, salt, iterations)
        if (MessageDigest.isEqual(sha256(key), verifier)) return key
        key.fill(0)
        return null
    }

    fun clear() = store.clear()

    private fun sha256(bytes: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(bytes)
}
