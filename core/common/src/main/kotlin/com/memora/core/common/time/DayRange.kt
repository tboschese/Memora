package com.memora.core.common.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Intervalo `[fromMs, toMs)` de um dia civil em epoch-millis. O banco não conhece fuso (ver os DAOs
 * `observeInRange`): é a camada de cima que traduz "hoje no fuso do usuário" para o intervalo que se
 * consulta. Meia-noite local inclusiva; meia-noite do dia seguinte exclusiva.
 *
 * Mora em `:core:common` porque serve a qualquer leitura por dia — a timeline de "Hoje" e a geração
 * do digest, entre outras — sem acoplar um feature a outro.
 */
data class DayRange(val fromMs: Long, val toMs: Long) {
    init {
        require(toMs >= fromMs) { "toMs ($toMs) deve ser >= fromMs ($fromMs)" }
    }

    companion object {
        /** Intervalo do dia que contém [instant] no fuso [zone]. */
        fun containing(instant: Instant, zone: ZoneId): DayRange =
            of(instant.atZone(zone).toLocalDate(), zone)

        /** Intervalo `[00:00, 24:00)` da data civil [date] no fuso [zone]. */
        fun of(date: LocalDate, zone: ZoneId): DayRange {
            val from = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val to = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            return DayRange(from, to)
        }
    }
}
