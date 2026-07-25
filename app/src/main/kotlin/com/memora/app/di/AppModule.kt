package com.memora.app.di

import com.memora.app.data.EncryptedSession
import com.memora.app.data.SecurityPinGate
import com.memora.app.data.SessionDatabaseHolder
import com.memora.app.session.SessionCoordinator
import com.memora.core.security.AutoLockController
import com.memora.core.security.PinVault
import com.memora.feature.onboarding.PinGate
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Fiação da sessão: liga o fluxo de PIN (`:feature:onboarding`) à segurança real e ao banco cifrado.
 * A `EncryptedSession` é o [SessionDatabaseHolder] (mesma instância singleton), então o gate abre o
 * banco ao autenticar e a UI o lê pelo holder. O `SecurityModule` já provê `PinVault`/`AutoLockController`.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun encryptedSession(holder: SessionDatabaseHolder): EncryptedSession = holder

    @Provides
    @Singleton
    fun pinGate(
        vault: PinVault,
        autoLock: AutoLockController,
        session: EncryptedSession,
    ): PinGate = SecurityPinGate(vault, autoLock, session)

    @Provides
    @Singleton
    fun sessionCoordinator(
        gate: PinGate,
        autoLock: AutoLockController,
    ): SessionCoordinator = SessionCoordinator(gate, autoLock)
}
