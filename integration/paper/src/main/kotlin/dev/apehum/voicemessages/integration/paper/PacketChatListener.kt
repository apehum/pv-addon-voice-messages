package dev.apehum.voicemessages.integration.paper

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisguisedChat
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage
import org.bukkit.Bukkit

class PacketChatListener : PacketListener {
    override fun onPacketSend(event: PacketSendEvent) {
        if (event.packetType == PacketType.Play.Server.DISGUISED_CHAT) {
            val packet = WrapperPlayServerDisguisedChat(event)
            val message = packet.message
            val user = event.user

            val player = Bukkit.getPlayer(user.uuid) ?: return

            packet.message =
                message.replaceText(
                    createVoiceMessageReplacement(player),
                )

            event.markForReEncode(true)
        } else if (event.packetType == PacketType.Play.Server.SYSTEM_CHAT_MESSAGE) {
            val packet = WrapperPlayServerSystemChatMessage(event)
            val message = packet.message
            val user = event.user

            val player = Bukkit.getPlayer(user.uuid) ?: return

            packet.message =
                message.replaceText(
                    createVoiceMessageReplacement(player),
                )
            event.markForReEncode(true)
        }
    }
}
