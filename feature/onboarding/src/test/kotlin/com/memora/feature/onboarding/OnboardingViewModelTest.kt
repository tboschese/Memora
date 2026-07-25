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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `propose then confirm creates the pin and finishes`() = runTest(dispatcher) {
        val gate = FakePinGate()
        val vm = OnboardingViewModel(gate)

        vm.propose("2468".toCharArray())
        assertEquals(SetupStep.CONFIRM, vm.uiState.value.step)

        vm.confirm("2468".toCharArray())
        advanceUntilIdle()

        assertEquals(SetupStep.DONE, vm.uiState.value.step)
        assertEquals(1, gate.setupCalls)
        assertEquals("2468", gate.lastSetupPin)
    }

    @Test
    fun `invalid pin keeps CREATE with a form error and never reaches the gate`() = runTest(dispatcher) {
        val gate = FakePinGate()
        val vm = OnboardingViewModel(gate)

        vm.propose("12".toCharArray())

        assertEquals(SetupStep.CREATE, vm.uiState.value.step)
        assertEquals(PinError.TOO_SHORT, vm.uiState.value.error)
        assertEquals(0, gate.setupCalls)
    }

    @Test
    fun `mismatched confirmation returns to CREATE and does not create`() = runTest(dispatcher) {
        val gate = FakePinGate()
        val vm = OnboardingViewModel(gate)

        vm.propose("1234".toCharArray())
        vm.confirm("9999".toCharArray())
        advanceUntilIdle()

        assertEquals(SetupStep.CREATE, vm.uiState.value.step)
        assertEquals(PinError.MISMATCH, vm.uiState.value.error)
        assertEquals(0, gate.setupCalls)
    }

    @Test
    fun `confirm without a proposal is a mismatch, not a crash`() = runTest(dispatcher) {
        val vm = OnboardingViewModel(FakePinGate())

        vm.confirm("1234".toCharArray())
        advanceUntilIdle()

        assertEquals(SetupStep.CREATE, vm.uiState.value.step)
        assertEquals(PinError.MISMATCH, vm.uiState.value.error)
    }

    @Test
    fun `restart clears error and returns to the beginning`() = runTest(dispatcher) {
        val vm = OnboardingViewModel(FakePinGate())
        vm.propose("12".toCharArray()) // sets an error

        vm.restart()

        assertEquals(SetupStep.CREATE, vm.uiState.value.step)
        assertNull(vm.uiState.value.error)
    }
}
