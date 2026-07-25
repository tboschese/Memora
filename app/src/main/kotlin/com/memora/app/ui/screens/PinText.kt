package com.memora.app.ui.screens

import com.memora.feature.onboarding.PinError

/** Texto amigável para cada erro de PIN mostrado nas telas. */
internal fun PinError.message(): String = when (this) {
    PinError.TOO_SHORT -> "PIN muito curto (mínimo 4 dígitos)."
    PinError.TOO_LONG -> "PIN muito longo (máximo 8 dígitos)."
    PinError.NOT_NUMERIC -> "Use apenas dígitos."
    PinError.MISMATCH -> "Os PINs não coincidem. Tente de novo."
    PinError.WRONG_PIN -> "PIN incorreto."
}
