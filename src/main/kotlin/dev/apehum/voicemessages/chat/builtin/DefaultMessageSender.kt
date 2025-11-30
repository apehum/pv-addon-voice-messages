package dev.apehum.voicemessages.chat.builtin

import dev.apehum.voicemessages.AddonConfig
import dev.apehum.voicemessages.api.VoiceMessage
import dev.apehum.voicemessages.api.chat.ChatContext
import dev.apehum.voicemessages.api.chat.ChatMessageSender
import dev.apehum.voicemessages.api.command.dsl.CommandContext
import dev.apehum.voicemessages.playback.component
import dev.apehum.voicemessages.util.extension.miniMessage
import dev.apehum.voicemessages.util.extension.toAdventure
import dev.apehum.voicemessages.util.extension.toMc
import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.server.McServerLib
import su.plo.slib.api.server.entity.player.McServerPlayer
import su.plo.slib.libs.adventure.adventure.text.minimessage.tag.resolver.Placeholder
import java.util.concurrent.CompletableFuture

data class DefaultChatContext(
    val source: McCommandSource,
) : ChatContext

class DefaultMessageSender(
    private val minecraftServer: McServerLib,
    private val formats: AddonConfig.ChatFormatConfig,
) : ChatMessageSender<DefaultChatContext> {
    override fun sendVoiceMessage(
        context: DefaultChatContext,
        message: VoiceMessage,
    ) {
        val sourceName =
            when (context.source) {
                is McServerPlayer -> context.source.name
                else -> "Console"
            }

        val voiceMessageComponent = message.component().toAdventure()

        val message =
            formats.default
                .miniMessage(
                    Placeholder.parsed("player_name", sourceName),
                    Placeholder.component("voice_message", voiceMessageComponent),
                ).toMc()

        minecraftServer.players.forEach { player ->
            player.sendMessage(message)
        }
    }

    override fun canSendMessage(context: DefaultChatContext): Boolean = true

    override fun createContext(context: CommandContext): CompletableFuture<DefaultChatContext> =
        CompletableFuture.completedFuture(DefaultChatContext(context.source))
}
