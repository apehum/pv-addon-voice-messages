package dev.apehum.voicemessages.channel.server

import dev.apehum.voicemessages.AddonConfig
import dev.apehum.voicemessages.VoiceMessagesAddon
import dev.apehum.voicemessages.api.VoiceMessagesAPI
import dev.apehum.voicemessages.api.chat.ChatContext
import dev.apehum.voicemessages.api.chat.ChatMessageSender
import dev.apehum.voicemessages.api.command.dsl.argument.StringReader
import dev.apehum.voicemessages.channel.UploadResult
import dev.apehum.voicemessages.channel.VoiceMessageUploads
import dev.apehum.voicemessages.channel.codec.FRAME_DURATION_MS
import dev.apehum.voicemessages.channel.codec.HandshakeRequest
import dev.apehum.voicemessages.channel.codec.HandshakeResponse
import dev.apehum.voicemessages.channel.codec.MAX_CHUNK_BYTES
import dev.apehum.voicemessages.channel.codec.MIN_PROTOCOL_VERSION
import dev.apehum.voicemessages.channel.codec.PROTOCOL_VERSION
import dev.apehum.voicemessages.channel.codec.Routes
import dev.apehum.voicemessages.channel.codec.SendFailure
import dev.apehum.voicemessages.channel.codec.SendParameters
import dev.apehum.voicemessages.channel.codec.SendResponse
import dev.apehum.voicemessages.channel.codec.SenderInfo
import dev.apehum.voicemessages.channel.codec.UploadRequest
import dev.apehum.voicemessages.channel.codec.isProtocolCompatible
import dev.apehum.voicemessages.command.dsl.DslCommandContext
import dev.apehum.voicemessages.command.dsl.DslParsedCommandArgument
import dev.apehum.voicemessages.record.createVoiceMessage
import dev.apehum.voicemessages.util.extension.canRecordWith
import kotlinx.coroutines.future.await
import su.plo.slib.api.server.entity.player.McServerPlayer
import su.plo.voice.api.server.PlasmoVoiceServer
import kotlin.time.Duration.Companion.seconds

private val UPLOAD_EXPIRE_AFTER = 30.seconds

fun ChannelServer.registerVoiceMessagesHandler(
    voiceServer: PlasmoVoiceServer,
    api: VoiceMessagesAPI,
    config: () -> AddonConfig,
) {
    val mtuSize = voiceServer.config?.voice()?.mtuSize() ?: 1024
    val uploads = VoiceMessageUploads(UPLOAD_EXPIRE_AFTER)

    registerRequestHandler<HandshakeRequest, HandshakeResponse>(Routes.HANDSHAKE) { player, request ->
        if (!isProtocolCompatible(request.protocolVersion, request.minProtocolVersion)) {
            VoiceMessagesAddon.logger.debug(
                "Refused {}: client protocol {}..{}, server {}..{}",
                player.name,
                request.minProtocolVersion,
                request.protocolVersion,
                MIN_PROTOCOL_VERSION,
                PROTOCOL_VERSION,
            )

            return@registerRequestHandler HandshakeResponse(PROTOCOL_VERSION, MIN_PROTOCOL_VERSION)
        }

        HandshakeResponse(
            protocolVersion = PROTOCOL_VERSION,
            minProtocolVersion = MIN_PROTOCOL_VERSION,
            maxDurationMs = config().maxDurationSeconds.seconds.inWholeMilliseconds,
            maxChunkBytes = MAX_CHUNK_BYTES,
            senders =
                api.messageSenderRegistry
                    .getSenders()
                    .filter { player.canRecordWith(it.key) }
                    .map { (name, sender) -> SenderInfo(name, sender.createArguments().map { it.name }) },
        )
    }

    registerRequestHandler<UploadRequest, SendResponse>(Routes.UPLOAD_CHUNK) { player, request ->
        val maxFrames = (config().maxDurationSeconds.seconds.inWholeMilliseconds / FRAME_DURATION_MS).toInt()
        val maxBytes = maxFrames * mtuSize

        val send =
            request.send ?: run {
                uploads.append(player, request, maxFrames, maxBytes)
                return@registerRequestHandler null
            }

        when (val result = uploads.complete(player, request, maxFrames, maxBytes)) {
            is UploadResult.Failed -> SendResponse(failure = result.failure)
            is UploadResult.Completed -> sendVoiceMessage(voiceServer, api, player, send, result.frames)
        }
    }
}

private suspend fun sendVoiceMessage(
    voiceServer: PlasmoVoiceServer,
    api: VoiceMessagesAPI,
    player: McServerPlayer,
    send: SendParameters,
    frames: List<ByteArray>,
): SendResponse {
    if (!player.canRecordWith(send.senderName)) return SendResponse(failure = SendFailure.NO_PERMISSION)

    @Suppress("UNCHECKED_CAST")
    val sender =
        api.messageSenderRegistry.getSender(send.senderName) as? ChatMessageSender<ChatContext>
            ?: return SendResponse(failure = SendFailure.UNKNOWN_SENDER)

    val chatContext =
        try {
            sender.createContext(sender.parseArguments(player, send.arguments)).await()
        } catch (e: Throwable) {
            VoiceMessagesAddon.logger.debug("Failed to parse arguments of \"{}\" for {}", send.senderName, player.name, e)
            return SendResponse(failure = SendFailure.BAD_ARGUMENTS)
        }

    if (!sender.canSendMessage(chatContext)) return SendResponse(failure = SendFailure.NO_PERMISSION)

    val decryptedFrames =
        try {
            frames.map(voiceServer.defaultEncryption::decrypt)
        } catch (e: Throwable) {
            VoiceMessagesAddon.logger.debug("Failed to decrypt a voice message from {}", player.name, e)
            return SendResponse(failure = SendFailure.BAD_REQUEST)
        }

    val voiceMessage =
        try {
            createVoiceMessage(voiceServer, decryptedFrames)
        } catch (e: Throwable) {
            VoiceMessagesAddon.logger.warn("Failed to decode a voice message from ${player.name}", e)
            return SendResponse(failure = SendFailure.INTERNAL)
        }

    api.messageStorage.save(voiceMessage)
    sender.sendVoiceMessage(chatContext, voiceMessage)

    return SendResponse(messageId = voiceMessage.id.toString())
}

private fun ChatMessageSender<*>.parseArguments(
    player: McServerPlayer,
    values: List<String>,
): DslCommandContext {
    val arguments =
        createArguments()
            .mapIndexed { index, (name, argument) ->
                val value =
                    requireNotNull(values.getOrNull(index)) { "Missing value for argument \"$name\"" }

                DslParsedCommandArgument(
                    name,
                    argument,
                    argument.parse(player, StringReader(value)),
                )
            }.associateBy { it.name }

    return DslCommandContext(player, arguments)
}
