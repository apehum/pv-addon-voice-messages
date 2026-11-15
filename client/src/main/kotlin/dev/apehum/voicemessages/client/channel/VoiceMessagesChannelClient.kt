package dev.apehum.voicemessages.client.channel

import dev.apehum.voicemessages.channel.codec.HandshakeRequest
import dev.apehum.voicemessages.channel.codec.HandshakeResponse
import dev.apehum.voicemessages.channel.codec.MIN_PROTOCOL_VERSION
import dev.apehum.voicemessages.channel.codec.PROTOCOL_VERSION
import dev.apehum.voicemessages.channel.codec.Routes
import dev.apehum.voicemessages.channel.codec.SendParameters
import dev.apehum.voicemessages.channel.codec.SendResponse
import dev.apehum.voicemessages.channel.codec.UploadRequest
import dev.apehum.voicemessages.channel.codec.isProtocolCompatible
import dev.apehum.voicemessages.client.BuildConfig
import kotlinx.coroutines.delay
import su.plo.slib.api.logging.McLoggerFactory
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val UPLOAD_TIMEOUT = 15.seconds

private val CHUNK_INTERVAL = 10.milliseconds

private val logger = McLoggerFactory.createLogger(BuildConfig.ADDON_ID)

class VoiceMessagesChannelClient(
    private val channel: ChannelClient,
) {
    private val handshakeRequest = channel.createRequest<HandshakeRequest, HandshakeResponse>(Routes.HANDSHAKE)
    private val uploadRequest = channel.createRequest<UploadRequest, SendResponse>(Routes.UPLOAD_CHUNK, UPLOAD_TIMEOUT)

    @Volatile
    var session: HandshakeResponse? = null
        private set

    suspend fun handshake(): HandshakeResponse? {
        session = null

        val response = handshakeRequest(HandshakeRequest(PROTOCOL_VERSION, MIN_PROTOCOL_VERSION))

        session =
            response.takeIf { isProtocolCompatible(it.protocolVersion, it.minProtocolVersion) }
                ?: run {
                    logger.warn(
                        "Voice messages protocol mismatch: server supports {}..{}, this mod supports {}..{}",
                        response.minProtocolVersion,
                        response.protocolVersion,
                        MIN_PROTOCOL_VERSION,
                        PROTOCOL_VERSION,
                    )
                    null
                }

        return session
    }

    suspend fun sendVoiceMessage(
        frames: List<ByteArray>,
        send: SendParameters,
    ): SendResponse {
        require(frames.isNotEmpty()) { "No frames to send" }
        val session = checkNotNull(session) { "No voice messages session" }

        val uploadId = UUID.randomUUID().toString()
        val chunks = frames.chunkedByBytes(session.maxChunkBytes)

        chunks.dropLast(1).forEachIndexed { index, chunk ->
            channel.sendMessage(Routes.UPLOAD_CHUNK, UploadRequest(uploadId, index, chunk))
            delay(CHUNK_INTERVAL)
        }

        return uploadRequest(UploadRequest(uploadId, chunks.lastIndex, chunks.last(), send))
    }

    fun reset() {
        session = null
        channel.reset()
    }
}

private fun List<ByteArray>.chunkedByBytes(maxBytes: Int): List<List<ByteArray>> {
    val chunks = mutableListOf<List<ByteArray>>()
    var chunk = mutableListOf<ByteArray>()
    var bytes = 0

    for (frame in this) {
        if (chunk.isNotEmpty() && bytes + frame.size > maxBytes) {
            chunks += chunk
            chunk = mutableListOf()
            bytes = 0
        }

        chunk += frame
        bytes += frame.size
    }

    if (chunk.isNotEmpty()) chunks += chunk

    return chunks
}
