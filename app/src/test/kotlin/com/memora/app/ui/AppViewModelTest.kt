package com.memora.app.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.memora.app.data.EncryptedSession
import com.memora.app.data.SecurityPinGate
import com.memora.app.data.SessionDatabaseHolder
import com.memora.app.session.SessionCoordinator
import com.memora.app.session.SessionPhase
import com.memora.core.security.AutoLockController
import com.memora.core.security.PinVault
import com.memora.core.security.fake.InMemorySecurityStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integração da navegação de sessão: o `AppViewModel` costura o fluxo de PIN (via `SecurityPinGate`
 * real sobre store em memória) ao `SessionCoordinator`, levando o app de onboarding a UNLOCKED.
 * Sem device — a sessão do banco (SQLCipher) é um fake que só registra a abertura.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AppViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private class FakeSession : EncryptedSession {
        var opened = false
        override fun open(key: ByteArray) { opened = true }
        override fun close() { opened = false }
    }

    private fun buildViewModel(configured: Boolean): AppViewModel {
        val store = InMemorySecurityStore()
        val vault = PinVault(store, iterations = 1_000)
        if (configured) vault.setup("2468".toCharArray())
        val autoLock = AutoLockController(timeoutMs = 60_000)
        val gate = SecurityPinGate(
            vault, autoLock, FakeSession(),
            clock = { 0L },
            derivationDispatcher = UnconfinedTestDispatcher(),
        )
        val coordinator = SessionCoordinator(gate, autoLock, clock = { 0L })
        val holder = SessionDatabaseHolder(ApplicationProvider.getApplicationContext<Application>())
        return AppViewModel(gate, coordinator, holder)
    }

    @Test
    fun `fresh install starts in onboarding`() {
        assertEquals(SessionPhase.ONBOARDING, buildViewModel(configured = false).phase.value)
    }

    @Test
    fun `creating a PIN unlocks the session`() = runTest(dispatcher) {
        val vm = buildViewModel(configured = false)

        vm.onboarding.propose("2468".toCharArray())
        vm.onboarding.confirm("2468".toCharArray())
        advanceUntilIdle()

        assertEquals(SessionPhase.UNLOCKED, vm.phase.value)
    }

    @Test
    fun `configured install starts locked and the right PIN unlocks`() = runTest(dispatcher) {
        val vm = buildViewModel(configured = true)
        assertEquals(SessionPhase.LOCKED, vm.phase.value)

        vm.unlock.submit("2468".toCharArray())
        advanceUntilIdle()

        assertEquals(SessionPhase.UNLOCKED, vm.phase.value)
    }

    @Test
    fun `a wrong PIN keeps the session locked`() = runTest(dispatcher) {
        val vm = buildViewModel(configured = true)

        vm.unlock.submit("0000".toCharArray())
        advanceUntilIdle()

        assertEquals(SessionPhase.LOCKED, vm.phase.value)
    }

    @Test
    fun `manual lock returns to the locked phase`() = runTest(dispatcher) {
        val vm = buildViewModel(configured = true)
        vm.unlock.submit("2468".toCharArray())
        advanceUntilIdle()
        assertEquals(SessionPhase.UNLOCKED, vm.phase.value)

        vm.lock()

        assertEquals(SessionPhase.LOCKED, vm.phase.value)
    }
}
