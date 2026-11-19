package dev.apehum.voicemessages.integration.paper

import dev.apehum.voicemessages.api.VoiceMessage
import dev.apehum.voicemessages.api.chat.ChatContext
import dev.apehum.voicemessages.api.chat.ChatMessageSender
import dev.apehum.voicemessages.api.command.dsl.CommandContext
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.server.entity.player.McServerPlayer
import java.util.concurrent.CompletableFuture

data class PaperChatContext(
    val source: Player,
) : ChatContext

class PaperMessageSender(
    private val plugin: Plugin,
) : ChatMessageSender<PaperChatContext> {
    override fun sendVoiceMessage(
        context: PaperChatContext,
        message: VoiceMessage,
    ) {
        context.source.scheduler.run(
            plugin,
            {
                context.source.chat(message.chatPlaceholder())
            },
            null,
        )
    }

    override fun canSendMessage(context: PaperChatContext): Boolean = true

    override fun createContext(context: CommandContext): CompletableFuture<PaperChatContext> {
        val player = (context.source as McServerPlayer).getInstance<Player>()

        return CompletableFuture.completedFuture(PaperChatContext(player))
    }

    override fun displayName(source: McCommandSource): McTextComponent =
        McTextComponent.translatable("pv.addon.voice_messages.sender.default")
}
