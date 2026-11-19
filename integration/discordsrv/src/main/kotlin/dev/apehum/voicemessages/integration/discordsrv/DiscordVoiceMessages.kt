package dev.apehum.voicemessages.integration.discordsrv

import dev.apehum.voicemessages.api.VoiceMessage
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel
import github.scarsz.discordsrv.dependencies.jda.api.requests.RestAction
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataArray
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject
import github.scarsz.discordsrv.dependencies.jda.internal.requests.RestActionImpl
import github.scarsz.discordsrv.dependencies.jda.internal.requests.Route
import github.scarsz.discordsrv.dependencies.okhttp3.MediaType
import github.scarsz.discordsrv.dependencies.okhttp3.MultipartBody
import github.scarsz.discordsrv.dependencies.okhttp3.RequestBody
import github.scarsz.discordsrv.util.WebhookUtil
import org.bukkit.entity.Player
import java.util.Base64
import kotlin.math.roundToInt

private const val IS_VOICE_MESSAGE_FLAG = 1L shl 13
private const val VOICE_MESSAGE_FILE_NAME = "voice-message.ogg"

val Message.isVoiceMessage: Boolean
    get() = flagsRaw and IS_VOICE_MESSAGE_FLAG != 0L

fun TextChannel.sendVoiceMessage(
    voiceMessage: VoiceMessage,
    ogg: ByteArray,
    replyTo: Message?,
): RestAction<Void> {
    val attachment =
        DataObject
            .empty()
            .put("id", 0)
            .put("filename", VOICE_MESSAGE_FILE_NAME)
            .put("duration_secs", voiceMessage.duration.toMillis() / 1000.0)
            .put("waveform", voiceMessage.discordWaveform())

    val payload =
        DataObject
            .empty()
            .put("flags", IS_VOICE_MESSAGE_FLAG)
            .put("attachments", DataArray.empty().add(attachment))
    if (replyTo != null) {
        payload.put(
            "message_reference",
            DataObject.empty().put("message_id", replyTo.id).put("fail_if_not_exists", false),
        )
    }

    val body =
        MultipartBody
            .Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("payload_json", payload.toString())
            .addFormDataPart(
                "files[0]",
                VOICE_MESSAGE_FILE_NAME,
                RequestBody.create(MediaType.get("audio/ogg"), ogg),
            ).build()

    return RestActionImpl(jda, Route.Messages.SEND_MESSAGE.compile(id), body)
}

fun TextChannel.deliverWebhookVoiceMessage(
    player: Player,
    content: String,
    ogg: ByteArray,
) {
    WebhookUtil.deliverMessage(
        this,
        player,
        content,
        emptyList<MessageEmbed>(),
        mapOf(VOICE_MESSAGE_FILE_NAME to ogg.inputStream()),
        null,
    )
}

private fun VoiceMessage.discordWaveform(): String =
    Base64.getEncoder().encodeToString(
        waveform.map { (it * 255).roundToInt().coerceIn(0, 255).toByte() }.toByteArray(),
    )
