package com.memora.app.capture

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.memora.feature.today.CaptureController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementação real do [CaptureController] (o *seam* de start/stop da captura): liga/desliga o
 * [CaptureService]. O estado observável vem do [RecordingState], que o serviço atualiza.
 */
@Singleton
class ServiceCaptureController @Inject constructor(
    @ApplicationContext private val context: Context,
    recordingState: RecordingState,
) : CaptureController {

    override val isRecording: StateFlow<Boolean> = recordingState.isRecording

    override fun start() {
        ContextCompat.startForegroundService(context, Intent(context, CaptureService::class.java))
    }

    override fun stop() {
        context.stopService(Intent(context, CaptureService::class.java))
    }
}
