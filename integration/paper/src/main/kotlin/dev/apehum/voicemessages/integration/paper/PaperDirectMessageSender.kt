package dev.apehum.voicemessages.integration.paper

import dev.apehum.voicemessages.api.VoiceMessage
import dev.apehum.voicemessages.api.chat.ChatContext
import dev.apehum.voicemessages.api.chat.ChatMessageSender
import dev.apehum.voicemessages.api.command.dsl.CommandContext
import dev.apehum.voicemessages.api.command.dsl.argument.NamedCommandArgument
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import su.plo.slib.api.server.entity.player.McServerPlayer
import java.util.concurrent.CompletableFuture

data class PaperDirectChatContext(
    val source: Player,
    val target: String,
) : ChatContext

class PaperDirectMessageSender(
    private val plugin: Plugin,
) : ChatMessageSender<PaperDirectChatContext> {
    override fun sendVoiceMessage(
        context: PaperDirectChatContext,
        message: VoiceMessage,
    ) {
        context.source.scheduler.run(
            plugin,
            {
                context.source.performCommand("msg ${context.target} [vm:${message.id}]")
            },
            null,
        )
    }

    override fun canSendMessage(context: PaperDirectChatContext): Boolean = true

    override fun createContext(context: CommandContext): CompletableFuture<PaperDirectChatContext> {
        val player = (context.source as McServerPlayer).getInstance<Player>()
        val target = context.getArgumentValue<String>("target")
        val context = PaperDirectChatContext(player, target)

        return CompletableFuture.completedFuture(context)
    }

    override fun createArguments(): List<NamedCommandArgument<out Any>> =
        listOf(
            NamedCommandArgument("target", PlayerNameArgument()),
        )
}
