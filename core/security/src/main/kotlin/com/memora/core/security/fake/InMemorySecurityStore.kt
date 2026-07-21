package com.memora.core.security.fake

import com.memora.core.security.SecurityStore

/**
 * [SecurityStore] em memória para testes (e Previews). Referência de comportamento correto, sem
 * Android Keystore. Copia os arrays na escrita/leitura para não vazar referências mutáveis.
 */
class InMemorySecurityStore : SecurityStore {
    private var salt: ByteArray? = null
    private var verifier: ByteArray? = null

    override fun readSalt(): ByteArray? = salt?.copyOf()
    override fun writeSalt(salt: ByteArray) { this.salt = salt.copyOf() }
    override fun readVerifier(): ByteArray? = verifier?.copyOf()
    override fun writeVerifier(verifier: ByteArray) { this.verifier = verifier.copyOf() }

    override fun clear() {
        salt = null
        verifier = null
    }
}
