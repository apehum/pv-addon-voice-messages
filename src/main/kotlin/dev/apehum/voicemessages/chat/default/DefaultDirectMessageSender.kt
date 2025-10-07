package dev.apehum.voicemessages.chat.default

import dev.apehum.voicemessages.chat.ChatContext
import dev.apehum.voicemessages.chat.ChatMessageSender
import dev.apehum.voicemessages.command.dsl.DslCommandContext
import dev.apehum.voicemessages.command.dsl.PlayerArgument
import dev.apehum.voicemessages.command.dsl.argument.NamedCommandArgument
import dev.apehum.voicemessages.playback.VoiceMessage
import dev.apehum.voicemessages.playback.component
import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.server.McServerLib
import su.plo.slib.api.server.entity.player.McServerPlayer

data class DirectChatContext(
    val source: McCommandSource,
    val target: McServerPlayer,
) : ChatContext

open class DefaultDirectMessageSender(
    private val minecraftServer: McServerLib,
) : ChatMessageSender<DirectChatContext> {
    override suspend fun sendVoiceMessage(
        context: DirectChatContext,
        message: VoiceMessage,
    ) {
        val voiceMessageComponent = message.component()

        context.source.sendMessage(voiceMessageComponent)
        context.target.sendMessage(voiceMessageComponent)
    }

    override suspend fun canSendMessage(context: DirectChatContext): Boolean {
        if (context.source !is McServerPlayer) return true
        return context.source.canSee(context.target)
    }

    override suspend fun createContext(context: DslCommandContext): DirectChatContext {
        val target = context.getArgumentValue<McServerPlayer>("target")
        return DirectChatContext(context.source, target)
    }

    override fun createArguments(): List<NamedCommandArgument<out Any>> =
        listOf(
            NamedCommandArgument("source", PlayerArgument(minecraftServer)),
        )
}
