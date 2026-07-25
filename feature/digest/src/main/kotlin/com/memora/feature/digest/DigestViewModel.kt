package com.memora.feature.digest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.core.common.time.DayRange
import com.memora.core.digest.DigestInput
import com.memora.core.digest.DigestProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/**
 * ViewModel da tela de Digest (plano §5.5): gera, sob demanda, o resumo estruturado do dia a partir
 * das falas persistidas. Só orquestra — busca as fontes ([DigestSources]) e delega a síntese ao
 * [DigestProvider] (fake nos testes; LLM local na impl real). Um dia sem fontes vira [DigestUiState.Empty]
 * sem chamar o modelo; qualquer falha do provider degrada para [DigestUiState.Failed] em vez de
 * derrubar a tela — o digest é reprodutível, nunca crítico ao restante do app.
 */
class DigestViewModel(
    private val sources: DigestSources,
    private val provider: DigestProvider,
    private val glossaryTerms: List<String> = emptyList(),
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val range: DayRange = DayRange.containing(now, zone)
    private val epochDay: Long = now.atZone(zone).toLocalDate().toEpochDay()

    private val _uiState = MutableStateFlow<DigestUiState>(DigestUiState.Idle)
    val uiState: StateFlow<DigestUiState> = _uiState.asStateFlow()

    /** Dispara a geração. Ignora chamadas concorrentes enquanto uma já está em andamento. */
    fun generate() {
        if (_uiState.value is DigestUiState.Generating) return
        _uiState.value = DigestUiState.Generating
        viewModelScope.launch {
            val next = try {
                val daySources = sources.forDay(range)
                if (daySources.isEmpty()) {
                    DigestUiState.Empty
                } else {
                    val digest = provider.generate(DigestInput(epochDay, daySources, glossaryTerms))
                    DigestUiState.Ready(digest)
                }
            } catch (e: CancellationException) {
                throw e // não engolir o cancelamento da coroutine
            } catch (e: Exception) {
                DigestUiState.Failed
            }
            _uiState.value = next
        }
    }
}
