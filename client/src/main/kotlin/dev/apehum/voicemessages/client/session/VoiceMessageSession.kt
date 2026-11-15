package dev.apehum.voicemessages.client.session

import dev.apehum.voicemessages.channel.codec.HandshakeResponse
import dev.apehum.voicemessages.channel.codec.SendFailure
import dev.apehum.voicemessages.channel.codec.SenderInfo
import dev.apehum.voicemessages.client.ClientVoiceMessagesAddon
import dev.apehum.voicemessages.client.channel.VoiceMessagesChannelClient
import dev.apehum.voicemessages.client.record.VoiceMessageDraftPlayer
import dev.apehum.voicemessages.client.record.VoiceMessageRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.minecraft.network.chat.Component
import su.plo.lib.mod.client.chat.ClientChatUtil
import su.plo.voice.api.client.PlasmoVoiceClient
import kotlin.time.Duration.Companion.milliseconds

// there's no sender picker yet, so default is used
private const val DEFAULT_SENDER = "default"

class VoiceMessageSession(
    private val addon: ClientVoiceMessagesAddon,
    private val voiceClient: PlasmoVoiceClient,
    channel: VoiceMessagesChannelClient,
    private val scope: CoroutineScope,
    handshake: HandshakeResponse,
) {
    val recorder = VoiceMessageRecorder(voiceClient, handshake.maxDurationMs.milliseconds)
    val preview = VoiceMessageDraftPlayer(voiceClient, scope)

    private val uploader = VoiceMessageUploader(voiceClient, channel)

    private val defaultSender: SenderInfo? =
        handshake.senders.firstOrNull { it.name == DEFAULT_SENDER && it.arguments.isEmpty() }

    @Volatile
    private var sending: Boolean = false

    val isActive: Boolean
        get() = recorder.isRecording || recorder.draft != null

    init {
        voiceClient.eventBus.register(addon, recorder)
    }

    fun canRecord(): Boolean = defaultSender != null && recorder.canRecord()

    fun startRecording(): Boolean {
        preview.stop()
        return recorder.start()
    }

    fun discard() {
        preview.stop()
        recorder.cancel()
    }

    fun send() {
        val draft = recorder.draft ?: return
        val sender = defaultSender ?: return

        if (sending) return
        sending = true

        preview.stop()

        scope.launch {
            try {
                when (val result = uploader.upload(draft, sender)) {
                    UploadResult.Sent -> discard()
                    is UploadResult.Failed -> reportFailure(result.failure)
                }
            } finally {
                sending = false
            }
        }
    }

    fun close() {
        discard()
        voiceClient.eventBus.unregister(addon, recorder)
    }

    private fun reportFailure(failure: SendFailure) {
        ClientChatUtil.sendChatMessage(
            Component.translatable(
                "pv.addon.voice_messages.chat.send_failed.${failure.name.lowercase()}",
            ),
        )
    }
}
