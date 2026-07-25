package com.memora.app.data

import com.memora.core.glossary.GlossaryCorrector
import com.memora.core.transcription.TranscriptResult

/**
 * Aplica a correção do glossário (ponto 2 de 3 da injeção) a um resultado de transcrição, corrigindo
 * o texto de cada segmento. Vive em `:app` porque liga dois módulos que não se conhecem
 * (`:core:transcription` e `:core:glossary`); é o lambda que o `TranscriptionQueue.postProcess`
 * recebe. Puro: um `TranscriptResult` entra, outro corrigido sai.
 */
fun TranscriptResult.correctedBy(corrector: GlossaryCorrector): TranscriptResult =
    copy(segments = segments.map { segment -> segment.copy(text = corrector.correct(segment.text)) })
