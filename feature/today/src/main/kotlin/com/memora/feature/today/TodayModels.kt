package com.memora.feature.today

import com.memora.core.common.model.SpeakerLabel

/**
 * Modelos da tela "Hoje". Domínio próprio do feature — desacoplado das entidades do Room: o
 * adaptador em `:app` mapeia `SegmentEntity`/`TimelineGapEntity` para estes tipos, do mesmo modo
 * que `RoomSegmentSink` mapeia na direção da escrita. Assim a UI não conhece o schema do banco.
 */

/**
 * Um item da timeline do dia, ordenável por [atMs] (epoch-millis). Uma fala transcrita ([Utterance])
 * ou um buraco na timeline ([Gap]) — porque "nada some em silêncio": o que se perdeu também aparece.
 */
sealed interface TodayItem {
    /** Instante que posiciona o item na ordem cronológica. */
    val atMs: Long

    /** Uma fala transcrita. O speaker começa `UNKNOWN` e só muda com confiança (Fase 2). */
    data class Utterance(
        val id: String,
        val text: String,
        val startMs: Long,
        val endMs: Long,
        val speaker: SpeakerLabel,
        val place: String? = null,
    ) : TodayItem {
        override val atMs: Long get() = startMs
    }

    /** Um intervalo perdido na timeline (overflow de fila, áudio ausente, falha de transcrição). */
    data class Gap(
        val fromMs: Long,
        val toMs: Long,
        val reason: TodayGapReason,
    ) : TodayItem {
        override val atMs: Long get() = fromMs
    }
}

/**
 * Motivo de um [TodayItem.Gap], em vocabulário da UI. Mapeado do `reason` persistido (String) pelo
 * adaptador; um valor desconhecido (schema mais novo) degrada para [UNKNOWN] em vez de estourar.
 */
enum class TodayGapReason {
    QUEUE_OVERFLOW,
    AUDIO_MISSING,
    TRANSCRIBE_FAILED,
    UNKNOWN,
    ;

    companion object {
        /** Converte o `reason` persistido; qualquer rótulo fora do enum vira [UNKNOWN]. */
        fun fromPersisted(reason: String): TodayGapReason =
            entries.firstOrNull { it.name == reason } ?: UNKNOWN
    }
}

/**
 * Estado da tela "Hoje". `isLoading` cobre o primeiro emit dos flows; depois dele, uma lista vazia
 * é um dia genuinamente sem falas ([isEmpty]).
 */
data class TodayUiState(
    val items: List<TodayItem> = emptyList(),
    val isRecording: Boolean = false,
    val isLoading: Boolean = true,
) {
    /** Dia sem nenhuma fala nem gap — distinto do carregamento inicial. */
    val isEmpty: Boolean get() = !isLoading && items.isEmpty()
}
