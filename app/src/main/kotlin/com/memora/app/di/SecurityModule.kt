package com.memora.app.di

import android.content.Context
import com.memora.core.security.AutoLockController
import com.memora.core.security.EncryptedPrefsSecurityStore
import com.memora.core.security.PinVault
import com.memora.core.security.SecurityStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI das peças de segurança que **não** dependem da chave em runtime — logo, seguras de prover no
 * grafo do app. O `MemoraDatabase` cifrado NÃO entra aqui: ele só pode ser aberto após o unlock
 * (a chave vem de `PinVault.unlock`), via `buildEncryptedDatabase` — parte do fluxo de onboarding.
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideSecurityStore(@ApplicationContext context: Context): SecurityStore =
        EncryptedPrefsSecurityStore(context)

    @Provides
    @Singleton
    fun providePinVault(store: SecurityStore): PinVault = PinVault(store)

    /** Estado único de auto-lock do app (regra 4: protege a leitura, não a captura). */
    @Provides
    @Singleton
    fun provideAutoLockController(): AutoLockController =
        AutoLockController(timeoutMs = DEFAULT_AUTO_LOCK_MS)

    private const val DEFAULT_AUTO_LOCK_MS = 120_000L // 2 min
}
