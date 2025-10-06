package dev.apehum.voicemessages.chat.default

import dev.apehum.voicemessages.chat.ChatMessageSender
import dev.apehum.voicemessages.playback.VoiceMessage
import dev.apehum.voicemessages.playback.component
import su.plo.slib.api.server.McServerLib
import su.plo.slib.api.server.entity.player.McServerPlayer

class DefaultGlobalMessageSender(
    private val minecraftServer: McServerLib,
) : ChatMessageSender {
    override suspend fun sendVoiceMessage(
        sender: McServerPlayer,
        message: VoiceMessage,
    ) {
        minecraftServer.players.forEach { player ->
            player.sendMessage(message.component())
        }
    }

    override suspend fun canSendMessage(sender: McServerPlayer): Boolean = true
}
