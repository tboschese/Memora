package com.memora.core.security

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * [SecurityStore] real: `SharedPreferences` cifradas pelo Android Keystore (AES-256). O salt e o
 * verifier ficam em repouso protegidos por hardware, elevando o custo de um ataque offline sobre
 * o PIN. Os bytes são guardados em Base64 (as prefs cifradas armazenam Strings).
 */
class EncryptedPrefsSecurityStore(context: Context) : SecurityStore {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        FILE,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun readSalt(): ByteArray? = read(KEY_SALT)
    override fun writeSalt(salt: ByteArray) = write(KEY_SALT, salt)
    override fun readVerifier(): ByteArray? = read(KEY_VERIFIER)
    override fun writeVerifier(verifier: ByteArray) = write(KEY_VERIFIER, verifier)

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private fun read(key: String): ByteArray? =
        prefs.getString(key, null)?.let { Base64.decode(it, Base64.NO_WRAP) }

    private fun write(key: String, value: ByteArray) {
        prefs.edit().putString(key, Base64.encodeToString(value, Base64.NO_WRAP)).apply()
    }

    companion object {
        private const val FILE = "memora_security"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_VERIFIER = "pin_verifier"
    }
}
