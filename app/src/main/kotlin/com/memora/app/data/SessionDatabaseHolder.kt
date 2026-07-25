package com.memora.app.data

import android.content.Context
import com.memora.core.db.MemoraDatabase
import com.memora.core.db.buildEncryptedDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda o [MemoraDatabase] cifrado da sessão: só existe **depois** do unlock (a chave vem de
 * `PinVault`), como manda o desenho — nada de banco no grafo de DI na construção. Implementa
 * [EncryptedSession], então o `SecurityPinGate` abre a sessão aqui ao autenticar; a UI lê os DAOs
 * via [database] enquanto a sessão está aberta.
 */
@Singleton
class SessionDatabaseHolder @Inject constructor(
    @ApplicationContext private val context: Context,
) : EncryptedSession {

    @Volatile
    private var db: MemoraDatabase? = null

    override fun open(key: ByteArray) {
        if (db == null) db = buildEncryptedDatabase(context, key)
    }

    override fun close() {
        db?.close()
        db = null
    }

    val isOpen: Boolean get() = db != null

    /** O banco da sessão. Só chame com a sessão aberta (fase UNLOCKED). */
    val database: MemoraDatabase
        get() = db ?: error("sessão do banco não aberta — unlock primeiro")
}
