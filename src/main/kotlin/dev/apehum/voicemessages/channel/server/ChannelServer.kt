package dev.apehum.voicemessages.channel.server

import dev.apehum.voicemessages.channel.codec.ChannelMessage
import dev.apehum.voicemessages.channel.codec.ChannelMessageRequest
import dev.apehum.voicemessages.channel.codec.ClientRequestHandler
import dev.apehum.voicemessages.channel.codec.channelMsgPack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import su.plo.slib.api.logging.McLoggerFactory
import su.plo.slib.api.server.channel.McServerChannelHandler
import su.plo.slib.api.server.channel.McServerChannelManager
import su.plo.slib.api.server.entity.player.McServerPlayer
import java.util.concurrent.ConcurrentHashMap

fun createChannelServer(
    channelKey: String,
    channelManager: McServerChannelManager,
): ChannelServer =
    ChannelServer(channelKey)
        .also { channelManager.registerChannelHandler(channelKey, it) }

class ChannelServer(
    val channelKey: String,
) : McServerChannelHandler {
    private val routes: MutableMap<String, Channel<ReceivedMessage>> = ConcurrentHashMap()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    inline fun <reified Request, reified Response> registerRequestHandler(
        route: String,
        handler: ClientRequestHandler<Request, Response?>,
    ) = registerHandler(route) { message ->
        val response =
            handler(message.player, channelMsgPack.decodeFromByteArray<Request>(message.payload))
                ?: return@registerHandler
        val requestId = message.requestId ?: return@registerHandler

        message.player.sendResponse(channelKey, requestId, response)
    }

    fun registerHandler(
        route: String,
        handler: suspend (ReceivedMessage) -> Unit,
    ) {
        // unlimited so receive() can hand off without launching a coroutine, which would reorder messages
        val channel = Channel<ReceivedMessage>(Channel.UNLIMITED)
        require(routes.putIfAbsent(route, channel) == null) { "Route $route is already registered" }

        scope.launch {
            for (message in channel) {
                try {
                    handler(message)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    logger.error("Failed to handle \"$route\" from ${message.player.name}", e)
                }
            }
        }
    }

    override fun receive(
        player: McServerPlayer,
        data: ByteArray,
    ) {
        val message =
            try {
                channelMsgPack.decodeFromByteArray<ChannelMessageRequest>(data)
            } catch (e: Throwable) {
                logger.warn("Failed to decode a message from ${player.name}", e)
                return
            }

        val channel = routes[message.route] ?: return
        channel.trySend(ReceivedMessage(player, message.requestId, message.payload))
    }

    fun shutdown() {
        scope.cancel()
        routes.values.forEach { it.close() }
        routes.clear()
    }

    class ReceivedMessage(
        val player: McServerPlayer,
        val requestId: String?,
        val payload: ByteArray,
    )

    private companion object {
        val logger = McLoggerFactory.createLogger("ChannelServer")
    }
}

@PublishedApi
internal inline fun <reified Response> McServerPlayer.sendResponse(
    channelKey: String,
    requestId: String,
    response: Response,
) {
    val message = ChannelMessage(requestId, channelMsgPack.encodeToByteArray(response))
    sendPacket(channelKey, channelMsgPack.encodeToByteArray(message))
}
