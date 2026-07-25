package com.memora.feature.onboarding

/**
 * O que pode dar errado num PIN, em vocabulário da UI. Cobre tanto a validação de forma
 * ([TOO_SHORT]/[TOO_LONG]/[NOT_NUMERIC]) quanto os erros de fluxo ([MISMATCH] na confirmação,
 * [WRONG_PIN] no desbloqueio).
 */
enum class PinError { TOO_SHORT, TOO_LONG, NOT_NUMERIC, MISMATCH, WRONG_PIN }

/**
 * Regras de forma do PIN. Pura e sem estado — o `CharArray` entra e nada é retido (a senha nunca
 * vira `String`, que ficaria no pool imutável da JVM). Retorna o primeiro problema, ou `null` se OK.
 */
object PinPolicy {
    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 8

    fun validate(pin: CharArray): PinError? = when {
        pin.size < MIN_LENGTH -> PinError.TOO_SHORT
        pin.size > MAX_LENGTH -> PinError.TOO_LONG
        !pin.all { it in '0'..'9' } -> PinError.NOT_NUMERIC
        else -> null
    }

    fun isValid(pin: CharArray): Boolean = validate(pin) == null
}
