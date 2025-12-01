package dev.apehum.voicemessages

import dev.apehum.voicemessages.api.VoiceMessage
import dev.apehum.voicemessages.api.VoiceMessagesAPI
import dev.apehum.voicemessages.api.VoiceMessagesAPIProvider
import dev.apehum.voicemessages.api.chat.ChatMessageSenderRegistry
import dev.apehum.voicemessages.api.event.MessageSenderRegistrationEvent
import dev.apehum.voicemessages.api.storage.draft.VoiceMessageDraftStorage
import dev.apehum.voicemessages.api.storage.message.VoiceMessageStorage
import dev.apehum.voicemessages.chat.builtin.DefaultDirectMessageSender
import dev.apehum.voicemessages.chat.builtin.DefaultMessageSender
import dev.apehum.voicemessages.command.LateInitCommand
import dev.apehum.voicemessages.command.voiceMessageActionsCommand
import dev.apehum.voicemessages.command.voiceMessageCommand
import dev.apehum.voicemessages.playback.VoiceMessagePlayer
import dev.apehum.voicemessages.playback.component
import dev.apehum.voicemessages.playback.ogg
import dev.apehum.voicemessages.record.VoiceActivationRecorder
import dev.apehum.voicemessages.record.VoiceMessageRecorder
import dev.apehum.voicemessages.storage.draft.MemoryVoiceMessageDraftStorage
import dev.apehum.voicemessages.storage.message.MemoryVoiceMessageStorage
import dev.apehum.voicemessages.storage.message.createJedisStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.logging.McLoggerFactory
import su.plo.slib.api.permission.PermissionDefault
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
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.minutes

@Addon(
    id = BuildConfig.PROJECT_NAME,
    version = BuildConfig.VERSION,
    authors = ["Apehum"],
    scope = AddonLoaderScope.SERVER,
)
class VoiceMessagesAddon :
    AddonInitializer,
    VoiceMessagesAPI {
    @InjectPlasmoVoice
    private lateinit var voiceServer: PlasmoVoiceServer

    private val minecraftServer: McServerLib by lazy {
        voiceServer.minecraftServer
    }

    private val addonFolder by lazy {
        minecraftServer.configsFolder.resolve(BuildConfig.PROJECT_NAME)
    }

    private lateinit var voiceRecorder: VoiceActivationRecorder
    private lateinit var voiceMessageRecorder: VoiceMessageRecorder
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

        VoiceMessagesAPIProvider.setInstance(this)
    }

    override fun onAddonInitialize() {
        messageSenderRegistry = ChatMessageSenderRegistry()

        val permissions = voiceServer.minecraftServer.permissionManager
        permissions.register("pv.addon.voice_messages.play", PermissionDefault.TRUE)
        permissions.register("pv.addon.voice_messages.record.default", PermissionDefault.TRUE)
        permissions.register("pv.addon.voice_messages.record.direct", PermissionDefault.TRUE)
        permissions.register("pv.addon.voice_messages.record.*", PermissionDefault.OP)

        reloadConfig()
    }

    override fun playVoiceMessage(
        player: VoiceServerPlayer,
        voiceMessage: VoiceMessage,
        showWaveform: Boolean,
    ) {
        messagePlayer.play(player.instance, voiceMessage, showWaveform)
    }

    override fun recordVoiceMessage(player: VoiceServerPlayer): CompletableFuture<VoiceMessage> =
        CoroutineScope(Dispatchers.Default).future {
            voiceMessageRecorder.record(player.instance)
        }

    override fun stopVoiceMessage(player: VoiceServerPlayer) = messagePlayer.cancel(player.instance)

    override fun createVoiceMessage(encodedFrames: List<ByteArray>): VoiceMessage =
        dev.apehum.voicemessages.record
            .createVoiceMessage(voiceServer, encodedFrames)

    override fun formatVoiceMessage(voiceMessage: VoiceMessage): McTextComponent = voiceMessage.component()

    override fun formatVoiceMessageToJson(voiceMessage: VoiceMessage): String =
        minecraftServer.textConverter.convertToJson(voiceMessage.component())

    override fun formatVoiceMessageToJson(
        voiceMessage: VoiceMessage,
        language: String,
    ): String = minecraftServer.textConverter.convertToJson(language, voiceMessage.component())

    override fun wrapVoiceMessageInOgg(voiceMessage: VoiceMessage): ByteArray = voiceMessage.ogg()

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
                    VoiceMessagesAddon::class.java.classLoader.getResourceAsStream("source_line_icon.png")!!,
                    config.sourceLineWeight,
                ).withPlayers(true) // this allows to show overlay when talking by default
                .build()

        messageSenderRegistry.register("default", DefaultMessageSender(voiceServer.minecraftServer, config.chatFormat))
        messageSenderRegistry.register("direct", DefaultDirectMessageSender(voiceServer.minecraftServer, config.chatFormat))
        MessageSenderRegistrationEvent.invoker.onRegistration(messageSenderRegistry)

        messageStorage =
            when (config.storageType) {
                AddonConfig.StorageType.MEMORY -> MemoryVoiceMessageStorage(config.expireAfterMinutes.minutes)
                AddonConfig.StorageType.REDIS -> {
                    requireNotNull(config.redis)
                    createJedisStore(config.redis, config.expireAfterMinutes.minutes)
                }
            }
        draftMessageStorage = MemoryVoiceMessageDraftStorage(config.expireAfterMinutes.minutes)
        logger.info("Voice message storage: ${config.storageType}")

        voiceRecorder =
            VoiceActivationRecorder(activation, voiceServer)
                .also { it.register(this) }
        voiceMessageRecorder = VoiceMessageRecorder(config, voiceServer, voiceRecorder)

        messagePlayer = VoiceMessagePlayer(sourceLine, voiceServer)

        voiceMessageCommand.initialize(
            voiceMessageCommand(
                voiceMessageCommand.name,
                voiceServer,
                voiceMessageRecorder,
                draftMessageStorage,
                messageSenderRegistry,
            ),
        )

        voiceMessageActionsCommand.initialize(
            voiceMessageActionsCommand(
                voiceServer,
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

    companion object {
        val logger = McLoggerFactory.createLogger(BuildConfig.PROJECT_NAME)
    }
}
