package dev.apehum.voicemessages.channel.codec

import kotlinx.serialization.Serializable

const val CHANNEL_KEY = "pv_addon_voice_messages:channel"

/**
 * Bump it only for a breaking change (renaming fields, changing field types).
 *
 * [HandshakeRequest] and [HandshakeResponse] must stay backward compatible:
 * they're how both sides find out that the other one is incompatible.
 */
const val PROTOCOL_VERSION = 1

const val MIN_PROTOCOL_VERSION = 1

const val MAX_CHUNK_BYTES = 16 * 1024

const val FRAME_DURATION_MS = 20L

fun isProtocolCompatible(
    theirVersion: Int,
    theirMin: Int,
): Boolean = theirVersion >= MIN_PROTOCOL_VERSION && theirMin <= PROTOCOL_VERSION

object Routes {
    const val HANDSHAKE = "handshake"
    const val UPLOAD_CHUNK = "upload_chunk"
    const val SUGGEST = "suggest"
    const val RESOLVE = "resolve"
}

@Serializable
data class HandshakeRequest(
    val protocolVersion: Int,
    val minProtocolVersion: Int,
)

@Serializable
data class HandshakeResponse(
    val protocolVersion: Int,
    val minProtocolVersion: Int,
    val maxDurationMs: Long = 0L,
    val maxChunkBytes: Int = 0,
    val senders: List<SenderInfo> = emptyList(),
)

@Serializable
data class SenderInfo(
    val name: String,
    val displayNameJson: String? = null,
    val arguments: List<ArgumentInfo> = emptyList(),
)

@Serializable
data class ArgumentInfo(
    val name: String,
    val displayNameJson: String? = null,
)

@Serializable
data class UploadRequest(
    val uploadId: String,
    val index: Int,
    val frames: List<ByteArray>,
    val send: SendParameters? = null,
)

@Serializable
data class SendParameters(
    val senderName: String,
    val arguments: String = "",
)

@Serializable
data class SendResponse(
    val messageId: String? = null,
    val failure: SendFailure? = null,
    val message: String? = null,
)

@Serializable
data class SuggestRequest(
    val senderName: String,
    val arguments: String,
)

@Serializable
data class SuggestResponse(
    val replaceFrom: Int = 0,
    val suggestions: List<String> = emptyList(),
)

@Serializable
data class ResolveRequest(
    val senderName: String,
    val arguments: String,
)

@Serializable
enum class SendFailure {
    NO_PERMISSION,
    UNKNOWN_SENDER,
    BAD_ARGUMENTS,
    BAD_REQUEST,
    UPLOAD_EXPIRED,
    TOO_LONG,
    INTERNAL,
}
