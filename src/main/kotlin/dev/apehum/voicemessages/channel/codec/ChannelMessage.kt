package dev.apehum.voicemessages.channel.codec

import kotlinx.serialization.Serializable
import su.plo.slib.api.server.entity.player.McServerPlayer

@Serializable
data class ChannelMessageRequest(
    val route: String,
    val requestId: String? = null,
    val payload: ByteArray,
)

@Serializable
data class ChannelMessage(
    val route: String,
    val payload: ByteArray,
)

fun interface ClientRequest<Request, Response> {
    suspend operator fun invoke(request: Request): Response
}

fun interface ClientRequestHandler<Request, Response> {
    suspend operator fun invoke(
        player: McServerPlayer,
        request: Request,
    ): Response
}
