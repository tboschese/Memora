package com.memora.core.audio

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * REGRA INVIOLÁVEL #2 — impl real em arquivo. Além do contrato de efemeridade (mesmo do fake em
 * memória), prova que após [EphemeralAudioStore.destroy] **nenhum arquivo sobrevive em disco** e
 * que o PCM em repouso está cifrado (não aparece em claro no arquivo).
 */
class FileEphemeralAudioStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun newStore(dir: File = tmp.newFolder()) = FileEphemeralAudioStore(dir)

    @Test
    fun `write e read fazem roundtrip`() = runTest {
        val store = newStore()
        val pcm = byteArrayOf(1, 2, 3, 4, 5)
        store.write("c1", pcm)
        assertArrayEquals(pcm, store.read("c1"))
        assertTrue("c1" in store.activeChunkIds())
    }

    @Test
    fun `destroy nao deixa residuo em disco`() = runTest {
        val dir = tmp.newFolder()
        val store = FileEphemeralAudioStore(dir)
        store.write("c1", ByteArray(2048) { 7 })

        store.destroy("c1")

        assertNull(store.read("c1"))
        assertTrue(store.activeChunkIds().isEmpty())
        // Prova direta no filesystem: nenhum arquivo de chunk permanece.
        val leftovers = dir.listFiles()?.filter { it.isFile }.orEmpty()
        assertTrue("nenhum resíduo deve sobrar: $leftovers", leftovers.isEmpty())
    }

    @Test
    fun `pcm em repouso esta cifrado`() = runTest {
        val dir = tmp.newFolder()
        val store = FileEphemeralAudioStore(dir)
        val marker = "SEGREDO-EM-CLARO".toByteArray()
        store.write("c1", marker)

        val onDisk = dir.listFiles()!!.first { it.isFile }.readBytes()
        // O texto em claro não pode aparecer no arquivo cifrado.
        assertFalse(onDisk.toList().windowedContains(marker.toList()))
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
    fun `chunks sao destruidos independentemente`() = runTest {
        val store = newStore()
        store.write("a", byteArrayOf(1))
        store.write("b", byteArrayOf(2))
        store.destroy("a")
        assertNull(store.read("a"))
        assertTrue("b" in store.activeChunkIds())
    }

    private fun <T> List<T>.windowedContains(sub: List<T>): Boolean {
        if (sub.isEmpty() || sub.size > size) return false
        for (i in 0..size - sub.size) {
            if (subList(i, i + sub.size) == sub) return true
        }
        return false
    }
}
