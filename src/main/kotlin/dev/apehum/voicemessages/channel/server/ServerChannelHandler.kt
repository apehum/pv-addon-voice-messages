package dev.apehum.voicemessages.channel.server

import dev.apehum.voicemessages.AddonConfig
import dev.apehum.voicemessages.VoiceMessagesAddon
import dev.apehum.voicemessages.api.VoiceMessagesAPI
import dev.apehum.voicemessages.api.chat.ChatContext
import dev.apehum.voicemessages.api.chat.ChatMessageSender
import dev.apehum.voicemessages.channel.UploadResult
import dev.apehum.voicemessages.channel.VoiceMessageUploads
import dev.apehum.voicemessages.channel.codec.ArgumentInfo
import dev.apehum.voicemessages.channel.codec.FRAME_DURATION_MS
import dev.apehum.voicemessages.channel.codec.HandshakeRequest
import dev.apehum.voicemessages.channel.codec.HandshakeResponse
import dev.apehum.voicemessages.channel.codec.MAX_CHUNK_BYTES
import dev.apehum.voicemessages.channel.codec.MIN_PROTOCOL_VERSION
import dev.apehum.voicemessages.channel.codec.PROTOCOL_VERSION
import dev.apehum.voicemessages.channel.codec.ResolveRequest
import dev.apehum.voicemessages.channel.codec.Routes
import dev.apehum.voicemessages.channel.codec.SendFailure
import dev.apehum.voicemessages.channel.codec.SendParameters
import dev.apehum.voicemessages.channel.codec.SendResponse
import dev.apehum.voicemessages.channel.codec.SenderInfo
import dev.apehum.voicemessages.channel.codec.SuggestRequest
import dev.apehum.voicemessages.channel.codec.SuggestResponse
import dev.apehum.voicemessages.channel.codec.UploadRequest
import dev.apehum.voicemessages.channel.codec.isProtocolCompatible
import dev.apehum.voicemessages.command.dsl.ArgumentsParseResult
import dev.apehum.voicemessages.command.dsl.parseArguments
import dev.apehum.voicemessages.command.dsl.suggestArguments
import dev.apehum.voicemessages.record.createVoiceMessage
import dev.apehum.voicemessages.util.extension.canRecordWith
import dev.apehum.voicemessages.util.extension.toJson
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
                    .map { (name, sender) -> senderInfo(player, name, sender) },
        )
    }

    registerRequestHandler<SuggestRequest, SuggestResponse>(Routes.SUGGEST) { player, request ->
        val sender =
            findSender(api, player, request.senderName)
                ?: return@registerRequestHandler SuggestResponse(request.arguments.length)

        val (replaceFrom, suggestions) =
            suggestArguments(
                player,
                sender.createArguments(),
                request.arguments,
            )
        SuggestResponse(replaceFrom, suggestions)
    }

    registerRequestHandler<ResolveRequest, SendResponse>(Routes.RESOLVE) { player, request ->
        when (val result = resolveContext(api, player, request.senderName, request.arguments)) {
            is ResolveResult.Resolved -> SendResponse()
            is ResolveResult.Failed -> result.toResponse()
        }
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

private sealed interface ResolveResult {
    data class Resolved(
        val sender: ChatMessageSender<ChatContext>,
        val context: ChatContext,
    ) : ResolveResult

    data class Failed(
        val failure: SendFailure,
        val message: String? = null,
    ) : ResolveResult
}

private fun ResolveResult.Failed.toResponse() = SendResponse(failure = failure, message = message)

private fun senderInfo(
    player: McServerPlayer,
    name: String,
    sender: ChatMessageSender<*>,
) = SenderInfo(
    name = name,
    displayNameJson = sender.displayName(player)?.toJson(),
    arguments = sender.createArguments().map { ArgumentInfo(it.name, it.displayName(player)?.toJson()) },
)

private fun findSender(
    api: VoiceMessagesAPI,
    player: McServerPlayer,
    senderName: String,
): ChatMessageSender<*>? =
    api.messageSenderRegistry
        .getSender(senderName)
        ?.takeIf { player.canRecordWith(senderName) }

private suspend fun resolveContext(
    api: VoiceMessagesAPI,
    player: McServerPlayer,
    senderName: String,
    arguments: String,
): ResolveResult {
    if (!player.canRecordWith(senderName)) return ResolveResult.Failed(SendFailure.NO_PERMISSION)

    @Suppress("UNCHECKED_CAST")
    val sender =
        api.messageSenderRegistry.getSender(senderName) as? ChatMessageSender<ChatContext>
            ?: return ResolveResult.Failed(SendFailure.UNKNOWN_SENDER)

    val commandContext =
        when (val result = parseArguments(player, sender.createArguments(), arguments)) {
            is ArgumentsParseResult.Parsed -> result.context
            is ArgumentsParseResult.Failed -> return ResolveResult.Failed(SendFailure.BAD_ARGUMENTS, result.message)
        }

    val chatContext =
        try {
            sender.createContext(commandContext).await()
        } catch (e: Throwable) {
            VoiceMessagesAddon.logger.debug("Failed to create context of \"{}\" for {}", senderName, player.name, e)
            return ResolveResult.Failed(SendFailure.BAD_ARGUMENTS, e.message)
        }

    if (!sender.canSendMessage(chatContext)) return ResolveResult.Failed(SendFailure.NO_PERMISSION)

    return ResolveResult.Resolved(sender, chatContext)
}

private suspend fun sendVoiceMessage(
    voiceServer: PlasmoVoiceServer,
    api: VoiceMessagesAPI,
    player: McServerPlayer,
    send: SendParameters,
    frames: List<ByteArray>,
): SendResponse {
    val (sender, chatContext) =
        when (val result = resolveContext(api, player, send.senderName, send.arguments)) {
            is ResolveResult.Resolved -> result
            is ResolveResult.Failed -> return result.toResponse()
        }

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

    api.messageStorage.save(voiceMessage).await()
    sender.sendVoiceMessage(chatContext, voiceMessage)

    return SendResponse(messageId = voiceMessage.id.toString())
}
