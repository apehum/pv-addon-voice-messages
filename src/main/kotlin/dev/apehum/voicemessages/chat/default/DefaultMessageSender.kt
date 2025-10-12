package dev.apehum.voicemessages.chat.default

import dev.apehum.voicemessages.chat.ChatContext
import dev.apehum.voicemessages.chat.ChatMessageSender
import dev.apehum.voicemessages.command.dsl.DslCommandContext
import dev.apehum.voicemessages.playback.VoiceMessage
import dev.apehum.voicemessages.playback.component
import su.plo.slib.api.server.McServerLib

data object DefaultChatContext : ChatContext

class DefaultMessageSender(
    private val minecraftServer: McServerLib,
) : ChatMessageSender<DefaultChatContext> {
    override suspend fun sendVoiceMessage(
        context: DefaultChatContext,
        message: VoiceMessage,
    ) {
        minecraftServer.players.forEach { player ->
            player.sendMessage(message.component())
        }
    }

    override suspend fun canSendMessage(context: DefaultChatContext): Boolean = true

    override suspend fun createContext(context: DslCommandContext): DefaultChatContext = DefaultChatContext
}
