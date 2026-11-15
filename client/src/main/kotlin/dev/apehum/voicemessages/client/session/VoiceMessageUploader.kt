package dev.apehum.voicemessages.client.session

import dev.apehum.voicemessages.channel.codec.SendFailure
import dev.apehum.voicemessages.channel.codec.SendParameters
import dev.apehum.voicemessages.channel.codec.SenderInfo
import dev.apehum.voicemessages.client.BuildConfig
import dev.apehum.voicemessages.client.channel.VoiceMessagesChannelClient
import dev.apehum.voicemessages.client.record.VoiceMessageDraft
import su.plo.slib.api.logging.McLoggerFactory
import su.plo.voice.api.client.PlasmoVoiceClient
import kotlin.jvm.optionals.getOrNull

private val logger = McLoggerFactory.createLogger(BuildConfig.ADDON_ID)

sealed interface UploadResult {
    data object Sent : UploadResult

    data class Failed(
        val failure: SendFailure,
    ) : UploadResult
}

class VoiceMessageUploader(
    private val voiceClient: PlasmoVoiceClient,
    private val channel: VoiceMessagesChannelClient,
) {
    suspend fun upload(
        draft: VoiceMessageDraft,
        sender: SenderInfo,
    ): UploadResult {
        val serverInfo = voiceClient.serverInfo.getOrNull() ?: return UploadResult.Failed(SendFailure.INTERNAL)

        return try {
            val encryption = serverInfo.encryption.getOrNull()
            val frames =
                serverInfo
                    .createOpusEncoder(false)
                    .use(draft::encode)
                    .map { encryption?.encrypt(it) ?: it }

            val failure = channel.sendVoiceMessage(frames, SendParameters(sender.name)).failure
            if (failure == null) UploadResult.Sent else UploadResult.Failed(failure)
        } catch (e: Throwable) {
            logger.warn("Failed to send a voice message", e)
            UploadResult.Failed(SendFailure.INTERNAL)
        }
    }
}
