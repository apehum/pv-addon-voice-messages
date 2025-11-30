package dev.apehum.voicemessages.chat.builtin

import dev.apehum.voicemessages.AddonConfig
import dev.apehum.voicemessages.api.VoiceMessage
import dev.apehum.voicemessages.api.chat.ChatContext
import dev.apehum.voicemessages.api.chat.ChatMessageSender
import dev.apehum.voicemessages.api.command.dsl.CommandContext
import dev.apehum.voicemessages.api.command.dsl.argument.NamedCommandArgument
import dev.apehum.voicemessages.api.command.dsl.argument.PlayerArgument
import dev.apehum.voicemessages.playback.component
import dev.apehum.voicemessages.util.extension.miniMessage
import dev.apehum.voicemessages.util.extension.toAdventure
import dev.apehum.voicemessages.util.extension.toMc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.server.McServerLib
import su.plo.slib.api.server.entity.player.McServerPlayer
import su.plo.slib.libs.adventure.adventure.text.minimessage.tag.resolver.Placeholder
import java.util.concurrent.CompletableFuture

data class DirectChatContext(
    val source: McCommandSource,
    val target: McServerPlayer,
) : ChatContext

open class DefaultDirectMessageSender(
    private val minecraftServer: McServerLib,
    private val formats: AddonConfig.ChatFormatConfig,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) : ChatMessageSender<DirectChatContext> {
    override fun sendVoiceMessage(
        context: DirectChatContext,
        message: VoiceMessage,
    ) {
        val sourceName =
            when (context.source) {
                is McServerPlayer -> context.source.name
                else -> "Console"
            }

        val voiceMessageComponent = message.component().toAdventure()

        val tagResolvers =
            arrayOf(
                Placeholder.parsed("source_player_name", sourceName),
                Placeholder.parsed("target_player_name", context.target.name),
                Placeholder.component("voice_message", voiceMessageComponent),
            )

        context.source.sendMessage(formats.directOutgoing.miniMessage(*tagResolvers).toMc())
        context.target.sendMessage(formats.directIncoming.miniMessage(*tagResolvers).toMc())
    }

    override fun canSendMessage(context: DirectChatContext): Boolean {
        if (context.source !is McServerPlayer) return true

        return context.source.canSee(context.target)
    }

    override fun createContext(context: CommandContext): CompletableFuture<DirectChatContext> =
        coroutineScope.future {
            val target = context.getArgumentValue<McServerPlayer>("target")
            DirectChatContext(context.source, target)
        }

    override fun createArguments(): List<NamedCommandArgument<out Any>> =
        listOf(
            NamedCommandArgument("target", PlayerArgument(minecraftServer)),
        )
}
