package com.memora.core.digest

import java.time.Instant
import java.time.ZoneId

/**
 * Política pura de quando gerar o digest do dia (RF-14/15/16). O digest é caro (LLM local), então a
 * regra prioriza o carregador sem perder o dia:
 * - antes de [targetHour] (21h): espera;
 * - entre [targetHour] e [deadlineHour] (23h): gera **se estiver carregando** (momento barato);
 * - a partir de [deadlineHour]: gera de qualquer forma, para o dia não ficar sem digest.
 *
 * Idempotente por dia: se já houve geração para a data civil de agora, não repete. O chamador
 * reavalia esta função tanto num alarme das 21h quanto no evento de "plugou o carregador".
 * Sem relógio interno nem I/O — `nowMs`/`charging`/`lastGeneratedEpochDay` entram por parâmetro.
 */
object DigestScheduler {
    const val TARGET_HOUR = 21
    const val DEADLINE_HOUR = 23

    fun shouldGenerate(
        nowMs: Long,
        zone: ZoneId,
        charging: Boolean,
        lastGeneratedEpochDay: Long?,
        targetHour: Int = TARGET_HOUR,
        deadlineHour: Int = DEADLINE_HOUR,
    ): Boolean {
        val dateTime = Instant.ofEpochMilli(nowMs).atZone(zone)
        val today = dateTime.toLocalDate().toEpochDay()
        if (lastGeneratedEpochDay == today) return false // já gerado hoje
        val hour = dateTime.hour
        if (hour < targetHour) return false // cedo demais
        return charging || hour >= deadlineHour
    }
}
