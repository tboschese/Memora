package com.memora.core.common.sync

/**
 * Seam de backup na nuvem — **opt-in, DESLIGADO por padrão** (local-first).
 *
 * Memora funciona 100% offline. Se o usuário optar por sincronizar (ex.: subir anotações para
 * o Google Drive), a implementação concreta vive atrás desta interface, é ativada explicitamente
 * nos Ajustes e é a ÚNICA porta pela qual dado sai do device. O default é [NoOpBackupProvider].
 *
 * Só texto/estruturado deve ser sincronizado — nunca áudio (que é efêmero).
 */
interface BackupProvider {

    /** Se o usuário ativou o backup. `false` = tudo permanece só no device. */
    val isEnabled: Boolean

    /** Sobe um snapshot (ex.: export Markdown do dia). Retorna sucesso/falha, sem lançar. */
    suspend fun backup(bundle: BackupBundle): BackupResult

    data class BackupBundle(
        val label: String,
        val contentType: String, // ex.: "text/markdown"
        val bytes: ByteArray,
    )

    sealed interface BackupResult {
        data class Success(val remoteId: String) : BackupResult
        data class Failure(val reason: String) : BackupResult
        data object Disabled : BackupResult
    }
}

/** Default local-first: não faz nada. Substituído por uma impl real (Drive/etc.) quando/opt-in. */
class NoOpBackupProvider : BackupProvider {
    override val isEnabled: Boolean = false
    override suspend fun backup(bundle: BackupProvider.BackupBundle) = BackupProvider.BackupResult.Disabled
}
