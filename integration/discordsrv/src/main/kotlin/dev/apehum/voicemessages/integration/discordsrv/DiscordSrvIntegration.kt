package dev.apehum.voicemessages.integration.discordsrv

import dev.apehum.voicemessages.api.VoiceMessage
import dev.apehum.voicemessages.api.VoiceMessagesAPIProvider
import dev.apehum.voicemessages.integration.paper.VOICE_MESSAGE_PLACEHOLDER
import dev.apehum.voicemessages.integration.paper.chatPlaceholder
import github.scarsz.discordsrv.DiscordSRV
import github.scarsz.discordsrv.api.Subscribe
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostProcessEvent
import github.scarsz.discordsrv.api.events.GameChatMessagePostProcessEvent
import github.scarsz.discordsrv.dependencies.jda.api.Permission
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.TextReplacementConfig
import github.scarsz.discordsrv.util.DiscordUtil
import org.bukkit.Bukkit
import java.util.UUID
import java.util.concurrent.CompletableFuture

class DiscordSrvIntegration(
    pluginName: String,
    private val voiceMessageLabel: () -> String,
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

    @Subscribe
    fun GameChatMessagePostProcessEvent.onGameChatMessage() {
        if (isCancelled) return
        val placeholder = VOICE_MESSAGE_PLACEHOLDER.find(processedMessage) ?: return
        val voiceMessageId = UUID.fromString(placeholder.groupValues[1])

        val discordSrv = DiscordSRV.getPlugin()
        val textChannel =
            discordSrv.getDestinationTextChannelForGameChannelName(channel ?: discordSrv.getOptionalChannel("global"))
                ?: return

        if (DiscordSRV.config().getBoolean("Experiment_WebhookChatMessageDelivery")) {
            if (!DiscordUtil.checkPermission(textChannel, Permission.MANAGE_WEBHOOKS)) return

            isCancelled = true
            val player = player
            val content = processedMessage.removeRange(placeholder.range).trim()

            findVoiceMessage(voiceMessageId)
                .thenAccept { voiceMessage ->
                    textChannel.deliverWebhookVoiceMessage(
                        player,
                        content,
                        voiceMessages.wrapVoiceMessageInOgg(voiceMessage),
                    )
                }.warnOnFailure(voiceMessageId)
        } else {
            isCancelled = true
            val line = processedMessage.replaceRange(placeholder.range, voiceMessageLabel())

            DiscordUtil.queueMessage(textChannel, line) { sentLine ->
                findVoiceMessage(voiceMessageId)
                    .thenCompose { voiceMessage ->
                        textChannel
                            .sendVoiceMessage(
                                voiceMessage,
                                voiceMessages.wrapVoiceMessageInOgg(voiceMessage),
                                sentLine,
                            ).submit()
                    }.warnOnFailure(voiceMessageId)
            }
        }
    }

    private fun findVoiceMessage(id: UUID): CompletableFuture<VoiceMessage> =
        voiceMessages.messageStorage
            .getById(id)
            .thenApply { checkNotNull(it) { "Voice message $id has expired" } }

    private fun CompletableFuture<*>.warnOnFailure(voiceMessageId: UUID) {
        whenComplete { _, error ->
            if (error != null) {
                logger.warn("Failed to send voice message {} to Discord", voiceMessageId, error)
            }
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
