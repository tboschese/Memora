package com.memora.core.db

import android.content.Context
import androidx.room.Room
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Abre o [MemoraDatabase] **cifrado** com SQLCipher (regra 3: só texto persiste, e cifrado no
 * device). A [passphrase] é a chave derivada do PIN em `:core:security` (`PinVault.unlock`) — este
 * módulo não conhece o PIN, só recebe a chave já pronta.
 *
 * Chamado após o unlock (não na construção do grafo de DI, pois a chave só existe em runtime). O
 * chamador deve zerar o array [passphrase] após a abertura.
 *
 * ⚠️ Depende da lib nativa do SQLCipher — roda em device/emulador, não sob Robolectric. Por isso
 * não há teste unitário aqui; o schema/DAOs são exercitados por testes com Room in-memory.
 */
fun buildEncryptedDatabase(context: Context, passphrase: ByteArray): MemoraDatabase {
    System.loadLibrary("sqlcipher")
    return Room.databaseBuilder(context, MemoraDatabase::class.java, MemoraDatabase.NAME)
        .openHelperFactory(SupportOpenHelperFactory(passphrase))
        .build()
}
