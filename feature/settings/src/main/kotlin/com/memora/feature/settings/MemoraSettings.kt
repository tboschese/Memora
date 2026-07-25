package com.memora.feature.settings

/**
 * Parâmetros avançados ajustáveis nos Ajustes (RF-34): os "botões" do Whisper/VAD/speaker/auto-lock
 * e do digest, num só lugar. Cada campo tem um default sensato ([DEFAULT]) e um intervalo válido; a
 * UI pode oferecer valores arbitrários, mas [normalized] os traz de volta ao intervalo antes de
 * qualquer uso — o pipeline nunca recebe um ajuste fora de faixa. Reset = voltar a [DEFAULT].
 *
 * Os defaults espelham os do código (ex.: `EnergyVad`, `VoiceProfile`, `AutoLockController`,
 * `DigestScheduler`); mantidos aqui como fonte única de configuração, sem acoplar os módulos.
 */
data class MemoraSettings(
    /** Idioma forçado do Whisper; `null` = auto-detect. */
    val transcriptionLanguage: String? = null,
    /** Similaridade mínima (cosseno) para SELF. */
    val speakerThreshold: Float = 0.65f,
    /** Margem abaixo do threshold em que a decisão vira UNKNOWN. */
    val speakerUnknownMargin: Float = 0.10f,
    /** RMS mínimo para o VAD considerar fala (energia). */
    val vadRmsThreshold: Double = 500.0,
    /** Timeout de auto-lock da leitura, em ms. */
    val autoLockTimeoutMs: Long = 120_000L,
    /** Hora-alvo do digest automático (0..23). */
    val digestTargetHour: Int = 21,
) {
    /** Traz cada campo ao seu intervalo válido. Idempotente: `x.normalized().normalized() == x.normalized()`. */
    fun normalized(): MemoraSettings = copy(
        transcriptionLanguage = transcriptionLanguage?.trim()?.lowercase()?.takeIf { it.isNotEmpty() },
        speakerThreshold = speakerThreshold.coerceIn(0f, 1f),
        // a margem não pode passar do threshold já normalizado nem ser negativa
        speakerUnknownMargin = speakerUnknownMargin.coerceIn(0f, speakerThreshold.coerceIn(0f, 1f)),
        vadRmsThreshold = vadRmsThreshold.coerceAtLeast(0.0),
        autoLockTimeoutMs = autoLockTimeoutMs.coerceAtLeast(MIN_AUTO_LOCK_MS),
        digestTargetHour = digestTargetHour.coerceIn(0, 23),
    )

    companion object {
        val DEFAULT = MemoraSettings()

        /** Auto-lock não pode ser instantâneo/negativo — mínimo de 5s para não trancar no meio de um toque. */
        const val MIN_AUTO_LOCK_MS = 5_000L
    }
}
