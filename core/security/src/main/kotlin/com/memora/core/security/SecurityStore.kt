package com.memora.core.security

/**
 * Persistência do material de setup do PIN: o **salt** e o **verifier**. NÃO guarda a chave do
 * banco nem o PIN em si. A impl real ([EncryptedPrefsSecurityStore]) cifra tudo com o Android
 * Keystore; o fake (`fake/InMemorySecurityStore`) é em memória, para testes.
 */
interface SecurityStore {
    fun readSalt(): ByteArray?
    fun writeSalt(salt: ByteArray)
    fun readVerifier(): ByteArray?
    fun writeVerifier(verifier: ByteArray)
    fun clear()
}
