package com.memora.feature.onboarding

/**
 * Gate fake em memória. `correctPin` define qual PIN o [unlock] aceita; registra as chamadas para o
 * teste inspecionar. Sem device, banco nem PBKDF2.
 */
class FakePinGate(
    private var configured: Boolean = false,
    private val correctPin: String? = null,
) : PinGate {
    var setupCalls: Int = 0
        private set
    var lastSetupPin: String? = null
        private set

    override fun isConfigured(): Boolean = configured

    override suspend fun setup(pin: CharArray) {
        setupCalls++
        lastSetupPin = String(pin)
        configured = true
    }

    override suspend fun unlock(pin: CharArray): Boolean = String(pin) == correctPin
}
