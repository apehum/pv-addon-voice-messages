package dev.apehum.voicemessages

import dev.apehum.voicemessages.chat.ChatMessageSenderRegistry
import dev.apehum.voicemessages.chat.default.DefaultDirectMessageSender
import dev.apehum.voicemessages.chat.default.DefaultMessageSender
import dev.apehum.voicemessages.command.LateInitCommand
import dev.apehum.voicemessages.command.voiceMessageActionsCommand
import dev.apehum.voicemessages.command.voiceMessageCommand
import dev.apehum.voicemessages.playback.VoiceMessagePlayer
import dev.apehum.voicemessages.record.VoiceActivationRecorder
import dev.apehum.voicemessages.store.draft.MemoryVoiceMessageDraftStore
import dev.apehum.voicemessages.store.draft.VoiceMessageDraftStore
import dev.apehum.voicemessages.store.message.MemoryVoiceMessageStore
import dev.apehum.voicemessages.store.message.VoiceMessageStore
import dev.apehum.voicemessages.store.message.createJedisStore
import su.plo.slib.api.logging.McLoggerFactory
import su.plo.slib.api.server.event.command.McServerCommandsRegisterEvent
import su.plo.voice.api.addon.AddonInitializer
import su.plo.voice.api.addon.AddonLoaderScope
import su.plo.voice.api.addon.InjectPlasmoVoice
import su.plo.voice.api.addon.annotation.Addon
import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.api.server.event.config.VoiceServerConfigReloadedEvent

@Addon(
    id = "pv-addon-voice-messages",
    version = BuildConstants.VERSION,
    authors = ["Apehum"],
    scope = AddonLoaderScope.SERVER,
)
class VoiceMessagesAddon : AddonInitializer {
    private val logger = McLoggerFactory.createLogger("pv-addon-voice-messages")

    @InjectPlasmoVoice
    private lateinit var voiceServer: PlasmoVoiceServer

    private lateinit var voiceRecorder: VoiceActivationRecorder
    private lateinit var messageStore: VoiceMessageStore
    private lateinit var draftStore: VoiceMessageDraftStore
    private lateinit var messagePlayer: VoiceMessagePlayer
    private lateinit var senderRegistry: ChatMessageSenderRegistry

    private val voiceMessageCommand = LateInitCommand("vm")
    private val voiceMessageActionsCommand = LateInitCommand("vm-actions")

    init {
        McServerCommandsRegisterEvent.registerListener { commandManager, _ ->
            voiceMessageCommand.register(commandManager)
            voiceMessageActionsCommand.register(commandManager)
        }
    }

    override fun onAddonInitialize() {
        senderRegistry = ChatMessageSenderRegistry()
        senderRegistry.register("default", DefaultMessageSender(voiceServer.minecraftServer))
        senderRegistry.register("direct", DefaultDirectMessageSender(voiceServer.minecraftServer))

        draftStore = MemoryVoiceMessageDraftStore()

        reloadConfig()
    }

    @EventSubscribe
    fun onConfigReload(event: VoiceServerConfigReloadedEvent) {
        reloadConfig()
    }

    private fun reloadConfig() {
        voiceServer.sourceLineManager.unregister("voice_messages")
        val oldVoiceRecorder = if (::voiceRecorder.isInitialized) voiceRecorder else null
        val oldMessagePlayer = if (::messagePlayer.isInitialized) messagePlayer else null

        val config = AddonConfig.loadConfig(voiceServer)

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

        messageStore =
            when (config.storageType) {
                AddonConfig.StorageType.MEMORY -> MemoryVoiceMessageStore()
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
                draftStore,
                senderRegistry,
            ),
        )

        voiceMessageActionsCommand.initialize(
            voiceMessageActionsCommand(
                voiceRecorder,
                messageStore,
                messagePlayer,
                draftStore,
                senderRegistry,
            ),
        )

        oldVoiceRecorder?.unregister(this)
        oldMessagePlayer?.clear()
    }
}
