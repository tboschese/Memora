package com.memora.feature.digest

import com.memora.core.digest.Digest

/**
 * Estado da tela de Digest. Sob demanda: parte de [Idle] e caminha para [Generating] e então
 * [Ready], [Empty] (dia sem nenhuma fala) ou [Failed] (o modelo local não produziu saída válida).
 */
sealed interface DigestUiState {
    data object Idle : DigestUiState
    data object Generating : DigestUiState
    data class Ready(val digest: Digest) : DigestUiState
    data object Empty : DigestUiState
    data object Failed : DigestUiState
}
