package dev.apehum.voicemessages.integration.discordsrv

import dev.apehum.voicemessages.api.VoiceMessage
import dev.apehum.voicemessages.api.VoiceMessagesAPIProvider
import dev.apehum.voicemessages.integration.paper.chatPlaceholder
import github.scarsz.discordsrv.DiscordSRV
import github.scarsz.discordsrv.api.Subscribe
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostProcessEvent
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.TextReplacementConfig
import org.bukkit.Bukkit

private const val IS_VOICE_MESSAGE_FLAG = 1L shl 13

private val Message.isVoiceMessage: Boolean
    get() = flagsRaw and IS_VOICE_MESSAGE_FLAG != 0L

class DiscordSrvIntegration(
    pluginName: String,
) {
    private val plugin = Bukkit.getPluginManager().getPlugin(pluginName)!!
    private val isDiscordSrvInstalled = plugin.server.pluginManager.getPlugin("DiscordSRV") != null
    private var isSubscribed = false

    private val voiceMessages by lazy { VoiceMessagesAPIProvider.getInstance() }

    private val logger by lazy { plugin.slF4JLogger }

    fun load() {
        if (!isDiscordSrvInstalled) return

        DiscordSRV.api.subscribe(this)
        isSubscribed = true
        logger.info("DiscordSRV integration loaded")
    }

    fun unload() {
        if (!isSubscribed) return

        DiscordSRV.api.unsubscribe(this)
        isSubscribed = false
    }

    @Subscribe
    fun DiscordGuildMessagePostProcessEvent.onGuildMessage() {
        if (isCancelled || !message.isVoiceMessage) return
        val attachment = message.attachments.singleOrNull() ?: return

        isCancelled = true

        val gameChannel = DiscordSRV.getPlugin().getDestinationGameChannelNameForTextChannel(channel)
        val author = author
        val originalMessage = minecraftMessage

        attachment
            .retrieveInputStream()
            .thenApply { stream -> voiceMessages.createVoiceMessage(stream.use { it.readOpusFrames() }) }
            .thenCompose { voiceMessage ->
                val component = originalMessage.replaceUrlWithPlaceholder(attachment.url, voiceMessage)

                voiceMessages.messageStorage
                    .save(voiceMessage)
                    .thenApply { component }
            }.handle { component, error ->
                if (error != null) {
                    logger.warn(
                        "Failed to convert Discord voice message {}, sending it as a link",
                        attachment.url,
                        error,
                    )
                }

                DiscordSRV.getPlugin().broadcastMessageToMinecraftServer(
                    gameChannel,
                    component ?: originalMessage,
                    author,
                )
            }
    }

    private fun Component.replaceUrlWithPlaceholder(
        url: String,
        voiceMessage: VoiceMessage,
    ): Component {
        val replaced =
            replaceText(
                TextReplacementConfig
                    .builder()
                    .matchLiteral(url)
                    .once()
                    .replacement(voiceMessage.chatPlaceholder())
                    .build(),
            )
        check(replaced != this) { "DiscordSRV message doesn't contain attachment url" }

        return replaced
    }
}
