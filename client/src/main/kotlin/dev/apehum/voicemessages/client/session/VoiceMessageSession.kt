package dev.apehum.voicemessages.client.session

import dev.apehum.voicemessages.channel.codec.HandshakeResponse
import dev.apehum.voicemessages.channel.codec.SendFailure
import dev.apehum.voicemessages.channel.codec.SendResponse
import dev.apehum.voicemessages.channel.codec.SuggestResponse
import dev.apehum.voicemessages.client.ClientVoiceMessagesAddon
import dev.apehum.voicemessages.client.channel.VoiceMessagesChannelClient
import dev.apehum.voicemessages.client.record.VoiceMessageDraftPlayer
import dev.apehum.voicemessages.client.record.VoiceMessageRecorder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import su.plo.lib.mod.client.chat.ClientChatUtil
import su.plo.voice.api.client.PlasmoVoiceClient
import kotlin.time.Duration.Companion.milliseconds

private const val DEFAULT_SENDER = "default"

class VoiceMessageSession(
    private val addon: ClientVoiceMessagesAddon,
    private val voiceClient: PlasmoVoiceClient,
    private val channel: VoiceMessagesChannelClient,
    private val scope: CoroutineScope,
    handshake: HandshakeResponse,
) {
    val recorder = VoiceMessageRecorder(voiceClient, handshake.maxDurationMs.milliseconds)
    val preview = VoiceMessageDraftPlayer(voiceClient, scope)

    val senders: List<ClientSender> = handshake.senders.map { it.toClientSender() }

    @Volatile
    var target: SendTarget? =
        (senders.firstOrNull { it.name == DEFAULT_SENDER && !it.hasArguments } ?: senders.firstOrNull { !it.hasArguments })
            ?.let(::SendTarget)
        private set

    private val uploader = VoiceMessageUploader(voiceClient, channel)

    @Volatile
    private var sending: Boolean = false

    val isActive: Boolean
        get() = recorder.isRecording || recorder.draft != null

    init {
        voiceClient.eventBus.register(addon, recorder)
    }

    fun canRecord(): Boolean = senders.isNotEmpty() && recorder.canRecord()

    fun startRecording(): Boolean {
        preview.stop()
        return recorder.start()
    }

    fun discard() {
        preview.stop()
        recorder.cancel()
    }

    fun send(target: SendTarget) {
        val draft = recorder.draft ?: return

        if (sending) return
        sending = true

        preview.stop()

        scope.launch {
            try {
                when (val result = uploader.upload(draft, target)) {
                    UploadResult.Sent -> {
                        this@VoiceMessageSession.target = target
                        discard()
                    }

                    is UploadResult.Failed -> {
                        reportFailure(result.failure, result.message)
                    }
                }
            } finally {
                sending = false
            }
        }
    }

    fun suggest(
        sender: ClientSender,
        arguments: String,
        onResult: (SuggestResponse) -> Unit,
    ): Job =
        scope.launch {
            val response =
                try {
                    channel.suggest(sender.name, arguments)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Throwable) {
                    return@launch
                }

            Minecraft.getInstance().execute { onResult(response) }
        }

    fun resolve(
        target: SendTarget,
        onResult: (SendResponse) -> Unit,
    ): Job =
        scope.launch {
            val response =
                try {
                    channel.resolve(target.sender.name, target.arguments)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Throwable) {
                    SendResponse(failure = SendFailure.INTERNAL)
                }

            Minecraft.getInstance().execute { onResult(response) }
        }

    fun close() {
        discard()
        voiceClient.eventBus.unregister(addon, recorder)
    }

    private fun reportFailure(
        failure: SendFailure,
        message: String?,
    ) {
        ClientChatUtil.sendChatMessage(failureText(failure, message))
    }
}

fun failureText(
    failure: SendFailure,
    message: String?,
): Component =
    message?.let(Component::literal)
        ?: Component.translatable("pv.addon.voice_messages.chat.send_failed.${failure.name.lowercase()}")
