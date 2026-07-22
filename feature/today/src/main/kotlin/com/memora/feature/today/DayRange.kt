package com.memora.feature.today

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Intervalo `[fromMs, toMs)` de um dia civil em epoch-millis. O banco não conhece fuso (ver
 * `SegmentDao.observeInRange`): é esta camada que traduz "hoje no fuso do usuário" para o intervalo
 * que os DAOs consultam. Meia-noite local inclusiva; meia-noite do dia seguinte exclusiva.
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
