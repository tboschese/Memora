package com.memora.app.data

import com.memora.core.speaker.VoiceProfile
import com.memora.core.transcription.WhisperOptions
import com.memora.feature.settings.MemoraSettings

/**
 * Deriva os parâmetros concretos do pipeline a partir de [MemoraSettings]. Vive em `:app` porque
 * cruza módulos que não se conhecem (settings ↔ transcription/speaker). Cada binding normaliza os
 * ajustes antes de usar, então valores fora de faixa (vindos da UI) nunca chegam ao pipeline.
 */

/** Opções do Whisper a partir dos ajustes; [initialPrompt] vem do glossário ([GlossaryPrompt]). */
fun MemoraSettings.toWhisperOptions(initialPrompt: String? = null): WhisperOptions {
    val s = normalized()
    return WhisperOptions(language = s.transcriptionLanguage, initialPrompt = initialPrompt)
}

/** Perfil de voz com os thresholds dos ajustes aplicados ao [embedding] do enrollment. */
fun MemoraSettings.toVoiceProfile(embedding: FloatArray): VoiceProfile {
    val s = normalized()
    return VoiceProfile(
        embedding = embedding,
        threshold = s.speakerThreshold,
        unknownMargin = s.speakerUnknownMargin,
    )
}
