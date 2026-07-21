package com.memora.core.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Impl real do [EphemeralAudioStore] em arquivo temporário (Fase 1). Herda o mesmo contrato de
 * efemeridade do fake em memória e adiciona a prova de **ausência de resíduo em disco** após
 * [destroy] (ver `FileEphemeralAudioStoreTest`).
 *
 * O PCM em repouso é cifrado com AES-256-GCM sob uma **chave efêmera gerada por processo**: ela
 * vive só em memória e some quando o processo morre, o que — mais do que a sobrescrita — é o que
 * torna qualquer resíduo irrecuperável. A destruição sobrescreve com zeros e apaga o arquivo.
 *
 * IO roda em [Dispatchers.IO]. Os arquivos ficam em [dir] (um diretório de cache dedicado, fora de
 * backups). Não guardamos PCM cru: só o blob cifrado, e só entre captura e transcrição.
 */
class FileEphemeralAudioStore(
    private val dir: File,
    private val key: SecretKey = generateEphemeralKey(),
    private val random: SecureRandom = SecureRandom(),
) : EphemeralAudioStore {

    init {
        dir.mkdirs()
    }

    override suspend fun write(chunkId: String, pcm: ByteArray): Unit = withContext(Dispatchers.IO) {
        val blob = encrypt(pcm)
        val target = fileFor(chunkId)
        val tmp = File(dir, "${target.name}.tmp")
        tmp.writeBytes(blob)
        // Move atômico para o nome final (evita ler um arquivo escrito pela metade).
        if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
    }

    override suspend fun read(chunkId: String): ByteArray? = withContext(Dispatchers.IO) {
        val f = fileFor(chunkId)
        if (!f.exists()) null else decrypt(f.readBytes())
    }

    override suspend fun destroy(chunkId: String): Unit = withContext(Dispatchers.IO) {
        val f = fileFor(chunkId)
        if (f.exists()) {
            val len = f.length()
            if (len > 0) {
                RandomAccessFile(f, "rws").use { raf ->
                    raf.seek(0)
                    raf.write(ByteArray(len.toInt()))
                }
            }
            f.delete()
        }
    }

    override suspend fun activeChunkIds(): Set<String> = withContext(Dispatchers.IO) {
        dir.listFiles { file -> file.isFile && file.name.endsWith(EXT) }
            ?.map { decodeId(it.name.removeSuffix(EXT)) }
            ?.toSet()
            .orEmpty()
    }

    private fun fileFor(chunkId: String) = File(dir, encodeId(chunkId) + EXT)

    // chunkId → nome de arquivo seguro (Base64 url-safe, reversível para activeChunkIds).
    private fun encodeId(chunkId: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(chunkId.toByteArray())

    private fun decodeId(name: String): String =
        String(Base64.getUrlDecoder().decode(name))

    private fun encrypt(plain: ByteArray): ByteArray {
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORM).apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        }
        return iv + cipher.doFinal(plain)
    }

    private fun decrypt(blob: ByteArray): ByteArray {
        val iv = blob.copyOfRange(0, IV_BYTES)
        val cipher = Cipher.getInstance(TRANSFORM).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        }
        return cipher.doFinal(blob, IV_BYTES, blob.size - IV_BYTES)
    }

    companion object {
        private const val EXT = ".pcm.enc"
        private const val TRANSFORM = "AES/GCM/NoPadding"
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128

        /** Chave AES-256 efêmera, só em memória — some com o processo. */
        fun generateEphemeralKey(): SecretKey =
            KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    }
}
