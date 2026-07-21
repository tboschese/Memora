package com.memora.core.security

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Derivação da chave do banco a partir do PIN (regra 3). PBKDF2-HMAC-SHA256 com alto custo de
 * iteração, para encarecer brute-force offline. A chave **nunca é persistida** — é re-derivada do
 * PIN a cada unlock (ver [PinVault]). Função pura e determinística: mesmo PIN + salt ⇒ mesma chave.
 */
object KeyDerivation {
    /** OWASP 2023 para PBKDF2-HMAC-SHA256. Ajustável em Ajustes se o device for lento. */
    const val DEFAULT_ITERATIONS = 210_000
    const val KEY_BITS = 256
    const val SALT_BYTES = 16

    fun deriveKey(
        pin: CharArray,
        salt: ByteArray,
        iterations: Int = DEFAULT_ITERATIONS,
        keyBits: Int = KEY_BITS,
    ): ByteArray {
        require(salt.isNotEmpty()) { "salt vazio" }
        require(iterations > 0) { "iterations deve ser > 0" }
        val spec = PBEKeySpec(pin, salt, iterations, keyBits)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
