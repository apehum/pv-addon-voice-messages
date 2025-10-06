package dev.apehum.voicemessages.chat

import dev.apehum.voicemessages.playback.VoiceMessage
import su.plo.slib.api.server.entity.player.McServerPlayer

interface ChatMessageSender {
    suspend fun sendVoiceMessage(
        sender: McServerPlayer,
        message: VoiceMessage,
    )

    suspend fun canSendMessage(sender: McServerPlayer): Boolean
}
