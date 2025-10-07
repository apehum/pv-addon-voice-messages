package dev.apehum.voicemessages.chat.default

import dev.apehum.voicemessages.chat.ChatContext
import dev.apehum.voicemessages.chat.ChatMessageSender
import dev.apehum.voicemessages.command.dsl.DslCommandContext
import dev.apehum.voicemessages.command.dsl.argument.NamedCommandArgument
import dev.apehum.voicemessages.playback.VoiceMessage
import dev.apehum.voicemessages.playback.component
import su.plo.slib.api.server.McServerLib
import su.plo.slib.api.server.entity.player.McServerPlayer

data object GlobalChatContext : ChatContext

class DefaultGlobalMessageSender(
    private val minecraftServer: McServerLib,
) : ChatMessageSender<GlobalChatContext> {
    override suspend fun sendVoiceMessage(
        context: GlobalChatContext,
        message: VoiceMessage,
    ) {
        minecraftServer.players.forEach { player ->
            player.sendMessage(message.component())
        }
    }

    override suspend fun canSendMessage(context: GlobalChatContext): Boolean = true

    override suspend fun createContext(context: DslCommandContext): GlobalChatContext = GlobalChatContext
}
