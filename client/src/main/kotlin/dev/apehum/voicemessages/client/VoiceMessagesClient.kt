package dev.apehum.voicemessages.client

import dev.apehum.voicemessages.client.record.VoiceMessageDraft
import dev.apehum.voicemessages.client.record.VoiceMessageDraftPlayer
import dev.apehum.voicemessages.client.record.VoiceMessageRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.minecraft.client.Minecraft
import su.plo.voice.api.client.PlasmoVoiceClient
import kotlin.time.Duration

object VoiceMessagesClient {
    // todo: replace with an actual handshake?
    private const val RECORD_COMMAND = "vm"

    private val scope = CoroutineScope(Dispatchers.Default)

    private var recorder: VoiceMessageRecorder? = null
    private var preview: VoiceMessageDraftPlayer? = null

    @JvmStatic
    val isRecording: Boolean
        get() = recorder?.isRecording == true

    val elapsed: Duration
        get() = recorder?.elapsed ?: Duration.ZERO

    val draft: VoiceMessageDraft?
        get() = recorder?.draft

    val isPreviewPlaying: Boolean
        get() = preview?.isPlaying == true

    val previewProgress: Double
        get() {
            val draft = draft ?: return 0.0
            val frames = draft.trimEnd - draft.trimStart
            if (frames <= 0) return 0.0

            return ((preview?.playedFrames ?: 0).toDouble() / frames).coerceIn(0.0, 1.0)
        }

    fun initialize(voiceClient: PlasmoVoiceClient): VoiceMessageRecorder =
        VoiceMessageRecorder(voiceClient).also {
            recorder = it
            preview = VoiceMessageDraftPlayer(voiceClient, scope)
        }

    @JvmStatic
    fun canRecord(): Boolean {
        val connection = Minecraft.getInstance().connection ?: return false
        if (connection.commands.root.getChild(RECORD_COMMAND) == null) return false

        return recorder?.canRecord() == true
    }

    @JvmStatic
    fun isActive(): Boolean = isRecording || draft != null

    @JvmStatic
    fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    fun recentLevels(count: Int): List<Double> = recorder?.recentLevels(count) ?: emptyList()

    fun startRecording(): Boolean {
        preview?.stop()
        return recorder?.start() == true
    }

    fun stopRecording(): VoiceMessageDraft? = recorder?.stop()

    fun playPreview(fromFrame: Int) {
        draft?.let { preview?.play(it, fromFrame) }
    }

    fun togglePreview() {
        val preview = preview ?: return

        if (preview.isPlaying) {
            preview.pause()
        } else {
            draft?.let { preview.play(it) }
        }
    }

    fun stopPreview() {
        preview?.stop()
    }

    fun send() {
        if (draft == null) return

        // todo: proper send
        discard()
    }

    @JvmStatic
    fun discard() {
        preview?.stop()
        recorder?.cancel()
    }
}
