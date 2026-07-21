package com.memora.core.audio

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * REGRA INVIOLÁVEL #2 — validação do contrato de áudio efêmero.
 *
 * Qualquer implementação de [EphemeralAudioStore] deve passar aqui. Na Fase 1, a impl
 * real (arquivo temporário) roda a MESMA bateria + prova adicional de ausência de resíduo
 * em disco após [EphemeralAudioStore.destroy].
 */
class EphemeralAudioContractTest {

    private fun newStore(): EphemeralAudioStore = InMemoryEphemeralAudioStore()

    @Test
    fun `chunk desaparece apos destroy`() = runTest {
        val store = newStore()
        store.write("c1", byteArrayOf(1, 2, 3))

        assertTrue("chunk deveria estar ativo antes do destroy", "c1" in store.activeChunkIds())

        store.destroy("c1")

        assertNull("read apos destroy deve retornar null", store.read("c1"))
        assertTrue("nenhum chunk pode sobreviver ao destroy", store.activeChunkIds().isEmpty())
    }

    @Test
    fun `destroy e idempotente`() = runTest {
        val store = newStore()
        store.write("c1", byteArrayOf(9))
        store.destroy("c1")
        store.destroy("c1") // não deve lançar
        assertTrue(store.activeChunkIds().isEmpty())
    }

    @Test
    fun `multiplos chunks sao destruidos independentemente`() = runTest {
        val store = newStore()
        store.write("a", byteArrayOf(1))
        store.write("b", byteArrayOf(2))

        store.destroy("a")

        assertNull(store.read("a"))
        assertTrue("b não deveria ser afetado", "b" in store.activeChunkIds())
    }
}
