package dev.apehum.voicemessages

import dev.apehum.voicemessages.api.VoiceMessagesAPI
import dev.apehum.voicemessages.api.VoiceMessagesAPIProvider
import dev.apehum.voicemessages.chat.ChatMessageSenderRegistry
import dev.apehum.voicemessages.chat.default.DefaultDirectMessageSender
import dev.apehum.voicemessages.chat.default.DefaultMessageSender
import dev.apehum.voicemessages.command.LateInitCommand
import dev.apehum.voicemessages.command.voiceMessageActionsCommand
import dev.apehum.voicemessages.command.voiceMessageCommand
import dev.apehum.voicemessages.playback.VoiceMessage
import dev.apehum.voicemessages.playback.VoiceMessagePlayer
import dev.apehum.voicemessages.record.VoiceActivationRecorder
import dev.apehum.voicemessages.storage.draft.MemoryVoiceMessageDraftStorage
import dev.apehum.voicemessages.storage.draft.VoiceMessageDraftStorage
import dev.apehum.voicemessages.storage.message.MemoryVoiceMessageStorage
import dev.apehum.voicemessages.storage.message.VoiceMessageStorage
import dev.apehum.voicemessages.storage.message.createJedisStore
import su.plo.slib.api.logging.McLoggerFactory
import su.plo.slib.api.server.McServerLib
import su.plo.slib.api.server.event.command.McServerCommandsRegisterEvent
import su.plo.voice.api.addon.AddonInitializer
import su.plo.voice.api.addon.AddonLoaderScope
import su.plo.voice.api.addon.InjectPlasmoVoice
import su.plo.voice.api.addon.annotation.Addon
import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.api.server.event.config.VoiceServerConfigReloadedEvent
import su.plo.voice.api.server.player.VoiceServerPlayer
import java.io.File
import java.io.InputStream

@Addon(
    id = BuildConfig.PROJECT_NAME,
    version = BuildConfig.VERSION,
    authors = ["Apehum"],
    scope = AddonLoaderScope.SERVER,
)
class VoiceMessagesAddon :
    AddonInitializer,
    VoiceMessagesAPI {
    private val logger = McLoggerFactory.createLogger(BuildConfig.PROJECT_NAME)

    @InjectPlasmoVoice
    private lateinit var voiceServer: PlasmoVoiceServer

    private val minecraftServer: McServerLib by lazy {
        voiceServer.minecraftServer
    }

    private val addonFolder by lazy {
        minecraftServer.configsFolder.resolve(BuildConfig.PROJECT_NAME)
    }

    private lateinit var voiceRecorder: VoiceActivationRecorder
    private lateinit var messagePlayer: VoiceMessagePlayer

    override lateinit var messageStorage: VoiceMessageStorage
    override lateinit var draftMessageStorage: VoiceMessageDraftStorage
    override lateinit var messageSenderRegistry: ChatMessageSenderRegistry

    private val voiceMessageCommand = LateInitCommand("vm")
    private val voiceMessageActionsCommand = LateInitCommand("vm-actions")

    init {
        McServerCommandsRegisterEvent.registerListener { commandManager, _ ->
            voiceMessageCommand.register(commandManager)
            voiceMessageActionsCommand.register(commandManager)
        }
    }

    override fun onAddonInitialize() {
        messageSenderRegistry = ChatMessageSenderRegistry()

        draftMessageStorage = MemoryVoiceMessageDraftStorage()

        reloadConfig()
        VoiceMessagesAPIProvider.setInstance(this)
    }

    override fun playVoiceMessage(
        player: VoiceServerPlayer,
        voiceMessage: VoiceMessage,
        showWaveform: Boolean,
    ) {
        messagePlayer.play(player.instance, voiceMessage, showWaveform)
    }

    override fun stopVoiceMessage(player: VoiceServerPlayer) = messagePlayer.cancel(player.instance)

    @EventSubscribe
    fun onConfigReload(event: VoiceServerConfigReloadedEvent) {
        reloadConfig()
    }

    private fun reloadConfig() {
        voiceServer.sourceLineManager.unregister("voice_messages")
        val oldVoiceRecorder = if (::voiceRecorder.isInitialized) voiceRecorder else null
        val oldMessagePlayer = if (::messagePlayer.isInitialized) messagePlayer else null

        val config = loadConfig<AddonConfig>(addonFolder)
        voiceServer.languages.register(
            ::getLanguageResource,
            File(addonFolder, "languages"),
        )

        val activation =
            voiceServer.activationManager
                .getActivationByName(config.activation)
                .orElseThrow { IllegalStateException("\"${config.activation}\" activation not found") }

        val sourceLine =
            voiceServer.sourceLineManager
                .createBuilder(
                    this,
                    "voice_messages",
                    "pv.addon.voice_messages.source_line",
                    config.sourceLine.icon,
                    config.sourceLine.weight,
                ).withPlayers(true) // this allows to show overlay when talking by default
                .build()

        messageSenderRegistry.register("default", DefaultMessageSender(voiceServer.minecraftServer, config.chatFormat))
        messageSenderRegistry.register("direct", DefaultDirectMessageSender(voiceServer.minecraftServer, config.chatFormat))

        messageStorage =
            when (config.storageType) {
                AddonConfig.StorageType.MEMORY -> MemoryVoiceMessageStorage()
                AddonConfig.StorageType.REDIS -> {
                    requireNotNull(config.redis)
                    createJedisStore(config.redis)
                }
            }
        logger.info("Voice message storage: ${config.storageType}")

        voiceRecorder =
            VoiceActivationRecorder(activation, voiceServer)
                .also { it.register(this) }

        messagePlayer = VoiceMessagePlayer(sourceLine, voiceServer)

        voiceMessageCommand.initialize(
            voiceMessageCommand(
                voiceMessageCommand.name,
                config,
                voiceServer,
                voiceRecorder,
                draftMessageStorage,
                messageSenderRegistry,
            ),
        )

        voiceMessageActionsCommand.initialize(
            voiceMessageActionsCommand(
                voiceRecorder,
                messageStorage,
                messagePlayer,
                draftMessageStorage,
                messageSenderRegistry,
            ),
        )

        oldVoiceRecorder?.unregister(this)
        oldMessagePlayer?.clear()
    }

    private fun getLanguageResource(resourcePath: String): InputStream? =
        VoiceMessagesAddon::class.java.classLoader.getResourceAsStream(String.format("voice_messages/%s", resourcePath))
}
