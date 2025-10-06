package dev.apehum.voicemessages.chat

import dev.apehum.voicemessages.playback.VoiceMessage
import su.plo.slib.api.server.entity.player.McServerPlayer

interface DirectMessageSender {
    suspend fun getOnlinePlayers(): List<String>

    suspend fun sendVoiceMessage(
        sender: McServerPlayer,
        targetNick: String,
        message: VoiceMessage,
    )
}
