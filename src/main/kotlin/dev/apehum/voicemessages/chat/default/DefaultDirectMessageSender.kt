package dev.apehum.voicemessages.chat.default

import dev.apehum.voicemessages.chat.ChatContext
import dev.apehum.voicemessages.chat.ChatMessageSender
import dev.apehum.voicemessages.command.dsl.DslCommandContext
import dev.apehum.voicemessages.command.dsl.PlayerArgument
import dev.apehum.voicemessages.command.dsl.argument.NamedCommandArgument
import dev.apehum.voicemessages.playback.VoiceMessage
import dev.apehum.voicemessages.playback.component
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.server.McServerLib
import su.plo.slib.api.server.entity.player.McServerPlayer
import java.util.concurrent.CompletableFuture

data class DirectChatContext(
    val source: McCommandSource,
    val target: McServerPlayer,
) : ChatContext

open class DefaultDirectMessageSender(
    private val minecraftServer: McServerLib,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) : ChatMessageSender<DirectChatContext> {
    override suspend fun sendVoiceMessage(
        context: DirectChatContext,
        message: VoiceMessage,
    ) {
        val sourceName =
            when (context.source) {
                is McServerPlayer -> context.source.name
                else -> "Console"
            }

        val voiceMessageComponent = message.component()

        context.source.sendMessage(
            McTextComponent.translatable(
                "pv.addon.voice_messages.chat_format.direct_outgoing",
                context.target.name,
                voiceMessageComponent,
            ),
        )

        context.target.sendMessage(
            McTextComponent.translatable(
                "pv.addon.voice_messages.chat_format.direct_incoming",
                sourceName,
                voiceMessageComponent,
            ),
        )
    }

    override fun canSendMessage(context: DirectChatContext): Boolean {
        if (context.source !is McServerPlayer) return true

        return context.source.canSee(context.target)
    }

    override fun createContext(context: DslCommandContext): CompletableFuture<DirectChatContext> =
        coroutineScope.future {
            val target = context.getArgumentValue<McServerPlayer>("target")
            DirectChatContext(context.source, target)
        }

    override fun createArguments(): List<NamedCommandArgument<out Any>> =
        listOf(
            NamedCommandArgument("target", PlayerArgument(minecraftServer)),
        )
}
