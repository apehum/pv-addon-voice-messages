@file:Suppress("UnstableApiUsage")

package dev.apehum.voicemessages.client.channel

import dev.apehum.voicemessages.channel.codec.ChannelMessage
import dev.apehum.voicemessages.channel.codec.ChannelMessageRequest
import dev.apehum.voicemessages.channel.codec.ClientRequest
import dev.apehum.voicemessages.channel.codec.channelMsgPack
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import net.minecraft.client.Minecraft
import net.minecraft.network.Connection
import net.minecraft.resources.ResourceLocation
import su.plo.slib.mod.channel.ModClientChannelHandler
import su.plo.slib.mod.channel.ModClientChannelManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val clientChannelManager = ModClientChannelManager.INSTANCE

val DEFAULT_TIMEOUT = 5.seconds

fun createChannelClient(channelKey: ResourceLocation): ChannelClient =
    ChannelClient(channelKey)
        .also {
            clientChannelManager.registerHandler(channelKey, it)
        }

class ChannelClient(
    val channelKey: ResourceLocation,
) : ModClientChannelHandler {
    val pending: MutableMap<String, Channel<ByteArray>> = ConcurrentHashMap()

    inline fun <reified Request, reified Response> createRequest(
        route: String,
        timeout: Duration = DEFAULT_TIMEOUT,
    ): ClientRequest<Request, Response> =
        ClientRequest { request ->
            val requestId = UUID.randomUUID().toString()
            val channel = Channel<ByteArray>(capacity = 1)

            pending[requestId] = channel

            try {
                send(ChannelMessageRequest(route, requestId, channelMsgPack.encodeToByteArray(request)))

                channelMsgPack.decodeFromByteArray<Response>(withTimeout(timeout) { channel.receive() })
            } finally {
                pending.remove(requestId)
                channel.close()
            }
        }

    inline fun <reified Message> sendMessage(
        route: String,
        message: Message,
    ) = send(ChannelMessageRequest(route, payload = channelMsgPack.encodeToByteArray(message)))

    fun send(request: ChannelMessageRequest) {
        val connection = Minecraft.getInstance().connection?.connection ?: error("Not connected")

        val payload = channelMsgPack.encodeToByteArray(request)
        clientChannelManager.sendPacket(channelKey, payload, connection)
    }

    override fun receive(
        payload: ByteArray,
        connection: Connection,
    ) {
        val message =
            try {
                channelMsgPack.decodeFromByteArray<ChannelMessage>(payload)
            } catch (_: Throwable) {
                return
            }

        pending[message.route]?.trySend(message.payload)
    }

    fun reset() {
        pending.values.forEach { it.close() }
        pending.clear()
    }
}
