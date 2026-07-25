package com.memora.core.models

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * [ModelRegistry] real sobre arquivos sideloaded numa pasta ([dir]). Sem rede: os modelos entram por
 * cópia manual (`.gguf`/`.onnx`). [statuses] é barato (só existência); [verify] recalcula o SHA-256 e
 * roda em [ioDispatcher] por ser I/O + hash de arquivos grandes.
 *
 * Um modelo ausente vira `present = false` (a feature degrada); presente com hash divergente vira
 * `checksumOk = false` (arquivo corrompido/errado) — nunca se usa um modelo que não confere.
 */
class FileModelRegistry(
    private val dir: File,
    private val specs: List<ModelSpec>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ModelRegistry {

    override fun statuses(): List<ModelStatus> =
        specs.map { spec -> ModelStatus(spec, present = fileOf(spec).isFile, checksumOk = null) }

    override suspend fun verify(): List<ModelStatus> = withContext(ioDispatcher) {
        specs.map { spec ->
            val file = fileOf(spec)
            if (!file.isFile) {
                ModelStatus(spec, present = false, checksumOk = null)
            } else {
                ModelStatus(spec, present = true, checksumOk = sha256(file).equals(spec.sha256, ignoreCase = true))
            }
        }
    }

    private fun fileOf(spec: ModelSpec): File = File(dir, spec.fileName)

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val DEFAULT_BUFFER_SIZE = 64 * 1024
    }
}
