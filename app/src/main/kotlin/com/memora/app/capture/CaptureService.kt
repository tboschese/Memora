package com.memora.app.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.memora.app.MainActivity
import com.memora.app.R
import com.memora.app.data.EphemeralAudioChunkAccess
import com.memora.app.data.RoomGapSink
import com.memora.app.data.RoomSegmentSink
import com.memora.app.data.SessionDatabaseHolder
import com.memora.core.audio.CapturePipeline
import com.memora.core.audio.EnergyVad
import com.memora.core.audio.FileEphemeralAudioStore
import com.memora.core.audio.PcmChunker
import com.memora.core.transcription.DeviceState
import com.memora.core.transcription.DrainMode
import com.memora.core.transcription.PendingChunk
import com.memora.core.transcription.TranscriptionQueue
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

/**
 * Captura de áudio contínua em primeiro plano (RF-01/RF-02). Lê PCM do microfone e o passa ao
 * [CapturePipeline] já testado (chunking → VAD → store efêmero cifrado), enfileira os chunks com
 * fala na [TranscriptionQueue] e persiste o texto no banco da sessão — o áudio é destruído logo após.
 *
 * Regra 2 (áudio efêmero): nada de PCM cru sobrevive à transcrição. Regra 4: roda em serviço próprio,
 * independente da leitura estar destrancada. A transcrição real (whisper.cpp) entra no *seam*
 * [PlaceholderTranscriptionProvider] sem tocar nesta fiação.
 */
@AndroidEntryPoint
class CaptureService : Service() {

    @Inject lateinit var holder: SessionDatabaseHolder
    @Inject lateinit var recordingState: RecordingState

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat()
        // Sem sessão aberta não há chave para cifrar o texto — não captura no vazio.
        if (!holder.isOpen) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (job == null) job = scope.launch { capture() }
        recordingState.set(true)
        return START_STICKY
    }

    private suspend fun capture() {
        val sampleRate = 16_000
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) return
        val bufferSize = minBuffer.coerceAtLeast(sampleRate) // ~0.5s

        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
            )
        } catch (e: SecurityException) {
            return // permissão de microfone não concedida
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return
        }

        val store = FileEphemeralAudioStore(File(cacheDir, "audio").also { it.mkdirs() })
        val access = EphemeralAudioChunkAccess(store, sampleRate)
        val dao = holder.database
        val queue = TranscriptionQueue(
            audio = access,
            provider = PlaceholderTranscriptionProvider(),
            segments = RoomSegmentSink(dao.segmentDao()),
            gaps = RoomGapSink(dao.timelineGapDao()),
        )
        val pipeline = CapturePipeline(
            chunker = PcmChunker(sampleRate = sampleRate, targetMs = CHUNK_MS),
            vad = EnergyVad(),
            store = store,
            captureStartMs = System.currentTimeMillis(),
            newChunkId = { UUID.randomUUID().toString() },
            onStored = { stored ->
                access.register(stored.chunkId, stored.startedAtMs, stored.durationMs)
                queue.enqueue(
                    PendingChunk(stored.chunkId, stored.startedAtMs, stored.durationMs, stored.sizeBytes.toLong()),
                )
            },
        )

        val device = DeviceState(charging = false, idle = false)
        val buffer = ShortArray(bufferSize)
        record.startRecording()
        try {
            while (scope.isActive) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    pipeline.onAudio(buffer.copyOf(read))
                    queue.drain(DrainMode.CONTINUOUS, device)
                }
            }
        } finally {
            record.stop()
            record.release()
            pipeline.stop()
            queue.drain(DrainMode.CONTINUOUS, device)
        }
    }

    override fun onDestroy() {
        job?.cancel()
        scope.cancel()
        recordingState.set(false)
        super.onDestroy()
    }

    private fun startForegroundCompat() {
        val open = android.app.PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Memora gravando")
            .setContentText("Capturando áudio. Só texto é guardado, cifrado.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(open)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Captura de áudio",
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private companion object {
        const val CHANNEL_ID = "memora_capture"
        const val NOTIF_ID = 1
        const val CHUNK_MS = 5_000L // 5s: feedback rápido; a spec pede 30–60s em produção
    }
}
