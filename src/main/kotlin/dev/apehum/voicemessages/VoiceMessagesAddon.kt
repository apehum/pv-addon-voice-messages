package dev.apehum.voicemessages

import dev.apehum.voicemessages.command.LateInitCommand
import dev.apehum.voicemessages.command.voiceMessageActionsCommand
import dev.apehum.voicemessages.command.voiceMessageCommand
import dev.apehum.voicemessages.playback.VoiceMessagePlayer
import dev.apehum.voicemessages.record.VoiceActivationRecorder
import dev.apehum.voicemessages.store.MemoryVoiceMessageStore
import dev.apehum.voicemessages.store.VoiceMessageStore
import su.plo.slib.api.server.event.command.McServerCommandsRegisterEvent
import su.plo.voice.api.addon.AddonInitializer
import su.plo.voice.api.addon.AddonLoaderScope
import su.plo.voice.api.addon.InjectPlasmoVoice
import su.plo.voice.api.addon.annotation.Addon
import su.plo.voice.api.server.PlasmoVoiceServer

@Addon(
    id = "pv-addon-voice-messages",
    version = BuildConstants.VERSION,
    authors = ["Apehum"],
    scope = AddonLoaderScope.SERVER,
)
class VoiceMessagesAddon : AddonInitializer {
    @InjectPlasmoVoice
    private lateinit var voiceServer: PlasmoVoiceServer

    private lateinit var voiceRecorder: VoiceActivationRecorder
    private lateinit var messageStore: VoiceMessageStore
    private lateinit var messagePlayer: VoiceMessagePlayer

    private val voiceMessageCommand = LateInitCommand("vm")
    private val voiceMessageActionsCommand = LateInitCommand("vm-actions")

    init {
        McServerCommandsRegisterEvent.registerListener { commandManager, _ ->
            voiceMessageCommand.register(commandManager)
            voiceMessageActionsCommand.register(commandManager)
        }
    }

    override fun onAddonInitialize() {
        val config = AddonConfig.loadConfig(voiceServer)

        val proximityActivation =
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

        voiceRecorder = VoiceActivationRecorder(proximityActivation, voiceServer).registerVoiceEvents()

        messageStore = MemoryVoiceMessageStore()
        messagePlayer = VoiceMessagePlayer(sourceLine, voiceServer)

        voiceMessageCommand.initialize(
            voiceMessageCommand(
                voiceMessageCommand.name,
                config,
                voiceServer,
                voiceRecorder,
                messageStore,
            ),
        )

        voiceMessageActionsCommand.initialize(
            voiceMessageActionsCommand(
                voiceRecorder,
                messageStore,
                messagePlayer,
            ),
        )
    }

    private fun <T : Any> T.registerVoiceEvents(): T =
        apply {
            voiceServer.eventBus.register(this@VoiceMessagesAddon, this)
        }
}
