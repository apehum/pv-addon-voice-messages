package dev.apehum.voicemessages.chat.default

import dev.apehum.voicemessages.chat.ChatContext
import dev.apehum.voicemessages.chat.ChatMessageSender
import dev.apehum.voicemessages.command.dsl.DslCommandContext
import dev.apehum.voicemessages.playback.VoiceMessage
import dev.apehum.voicemessages.playback.component
import dev.apehum.voicemessages.util.extension.sendTranslatable
import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.server.McServerLib
import su.plo.slib.api.server.entity.player.McServerPlayer

data class DefaultChatContext(
    val source: McCommandSource,
) : ChatContext

class DefaultMessageSender(
    private val minecraftServer: McServerLib,
) : ChatMessageSender<DefaultChatContext> {
    override suspend fun sendVoiceMessage(
        context: DefaultChatContext,
        message: VoiceMessage,
    ) {
        val sourceName =
            when (context.source) {
                is McServerPlayer -> context.source.name
                else -> "Console"
            }

        val voiceMessageComponent = message.component()

        minecraftServer.players.forEach { player ->
            player.sendTranslatable("pv.addon.voice_messages.chat_format.default", sourceName, voiceMessageComponent)
        }
    }

    override suspend fun canSendMessage(context: DefaultChatContext): Boolean = true

    override suspend fun createContext(context: DslCommandContext): DefaultChatContext = DefaultChatContext(context.source)
}
