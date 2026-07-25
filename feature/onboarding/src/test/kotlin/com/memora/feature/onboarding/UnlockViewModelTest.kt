package com.memora.feature.onboarding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UnlockViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `correct pin unlocks`() = runTest(dispatcher) {
        val vm = UnlockViewModel(FakePinGate(configured = true, correctPin = "1234"))

        vm.submit("1234".toCharArray())
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isUnlocked)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `wrong pin reports error and stays locked`() = runTest(dispatcher) {
        val vm = UnlockViewModel(FakePinGate(configured = true, correctPin = "1234"))

        vm.submit("0000".toCharArray())
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isUnlocked)
        assertEquals(PinError.WRONG_PIN, vm.uiState.value.error)
    }

    @Test
    fun `malformed pin is rejected as wrong without touching the gate`() = runTest(dispatcher) {
        // correctPin null: se chegasse ao gate, "12" nunca desbloquearia de qualquer forma;
        // aqui garantimos que a forma é barrada antes (sem submit em andamento).
        val vm = UnlockViewModel(FakePinGate(configured = true, correctPin = "12"))

        vm.submit("12".toCharArray()) // curto demais para a política
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isUnlocked)
        assertEquals(PinError.WRONG_PIN, vm.uiState.value.error)
        assertFalse(vm.uiState.value.isSubmitting)
    }
}
