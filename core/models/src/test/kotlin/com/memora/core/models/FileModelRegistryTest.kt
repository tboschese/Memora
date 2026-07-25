package com.memora.core.models

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest

class FileModelRegistryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun writeModel(name: String, content: ByteArray): File =
        tmp.newFile(name).apply { writeBytes(content) }

    private fun spec(name: String, sha: String) =
        ModelSpec(kind = ModelKind.TRANSCRIPTION, fileName = name, sha256 = sha)

    @Test
    fun `statuses reports presence without computing checksum`() {
        val content = "whisper".toByteArray()
        writeModel("whisper.gguf", content)
        val registry = FileModelRegistry(
            tmp.root,
            listOf(spec("whisper.gguf", sha256(content)), spec("absent.gguf", "deadbeef")),
        )

        val statuses = registry.statuses()
        assertTrue(statuses[0].present)
        assertNull(statuses[0].checksumOk) // não verificado ainda
        assertFalse(statuses[1].present)
    }

    @Test
    fun `verify confirms a matching checksum`() = runTest {
        val content = "silero-vad".toByteArray()
        writeModel("vad.onnx", content)
        val registry = FileModelRegistry(tmp.root, listOf(spec("vad.onnx", sha256(content))))

        val status = registry.verify().single()
        assertTrue(status.present)
        assertEquals(true, status.checksumOk)
    }

    @Test
    fun `verify flags a corrupted file`() = runTest {
        writeModel("model.gguf", "corrompido".toByteArray())
        val registry = FileModelRegistry(tmp.root, listOf(spec("model.gguf", sha256("original".toByteArray()))))

        val status = registry.verify().single()
        assertTrue(status.present)
        assertEquals(false, status.checksumOk)
    }

    @Test
    fun `verify leaves an absent model unverified`() = runTest {
        val registry = FileModelRegistry(tmp.root, listOf(spec("missing.gguf", "abcd")))

        val status = registry.verify().single()
        assertFalse(status.present)
        assertNull(status.checksumOk)
    }

    @Test
    fun `checksum comparison is case-insensitive`() = runTest {
        val content = "ecapa".toByteArray()
        writeModel("spk.onnx", content)
        val registry = FileModelRegistry(tmp.root, listOf(spec("spk.onnx", sha256(content).uppercase())))

        assertEquals(true, registry.verify().single().checksumOk)
    }

    @Test
    fun `a directory in place of a file counts as absent`() = runTest {
        File(tmp.root, "model.gguf").mkdirs()
        val registry = FileModelRegistry(tmp.root, listOf(spec("model.gguf", "abcd")))

        assertFalse(registry.verify().single().present)
    }
}
