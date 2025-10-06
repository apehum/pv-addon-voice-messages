package dev.apehum.voicemessages.command

import dev.apehum.voicemessages.AddonConfig
import dev.apehum.voicemessages.chat.ChatMessageSender
import dev.apehum.voicemessages.chat.default.DefaultGlobalMessageSender
import dev.apehum.voicemessages.command.dsl.DslCommand
import dev.apehum.voicemessages.command.dsl.dslCommand
import dev.apehum.voicemessages.playback.VoiceMessage
import dev.apehum.voicemessages.playback.createVoiceMessage
import dev.apehum.voicemessages.record.VoiceActivationRecorder
import dev.apehum.voicemessages.store.VoiceMessageStore
import dev.apehum.voicemessages.util.extension.padStartZero
import dev.apehum.voicemessages.util.extension.sendTranslatable
import dev.apehum.voicemessages.util.extension.sendTranslatableActionbar
import dev.apehum.voicemessages.util.extension.translateClientPV
import kotlinx.coroutines.TimeoutCancellationException
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.chat.style.McTextClickEvent
import su.plo.slib.api.server.entity.player.McServerPlayer
import su.plo.voice.api.server.PlasmoVoiceServer
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private suspend fun recordAndSaveVoiceMessage(
    player: McServerPlayer,
    config: AddonConfig,
    voiceServer: PlasmoVoiceServer,
    voiceRecorder: VoiceActivationRecorder,
    messageStore: VoiceMessageStore,
): VoiceMessage? {
    val timeoutDuration = 5.seconds

    player.sendTranslatable(
        "pv.addon.voice_messages.command.start_recording",
        player.translateClientPV(voiceServer, voiceRecorder.activation.translation),
        timeoutDuration.inWholeSeconds,
        McTextComponent
            .translatable("pv.addon.voice_messages.command.cancel_recording")
            .clickEvent(McTextClickEvent.runCommand("/vm-actions record-cancel")),
    )

    val maxDuration = config.maxDurationSeconds.seconds
    var maxDurationWarningSent = false

    val encodedFrames =
        try {
            voiceRecorder.stop(player, Exception("New recording started"))
            voiceRecorder.record(
                player,
                timeout = timeoutDuration,
                duration = maxDuration,
                frameCallback = { _, duration ->
                    val isFirstFrame = duration == 20.milliseconds
                    if (isFirstFrame) {
                        player.sendTranslatable(
                            "pv.addon.voice_messages.command.recording_started",
                            McTextComponent
                                .translatable("pv.addon.voice_messages.command.send_recording")
                                .clickEvent(McTextClickEvent.runCommand("/vm-actions record-stop")),
                            McTextComponent
                                .translatable("pv.addon.voice_messages.command.cancel_and_delete_recording")
                                .clickEvent(McTextClickEvent.runCommand("/vm-actions record-cancel")),
                        )
                    }

                    if (config.actionbarWhenRecording && (isFirstFrame || duration.inWholeMilliseconds % 1000L == 0L)) {
                        player.sendTranslatableActionbar(
                            "pv.addon.voice_messages.command.playback_actionbar",
                            duration.inWholeSeconds.padStartZero(),
                            maxDuration.inWholeSeconds.padStartZero(),
                        )
                    }

                    if (!maxDurationWarningSent && maxDuration.inWholeSeconds - duration.inWholeSeconds <= 10) {
                        maxDurationWarningSent = true
                        player.sendTranslatable(
                            "pv.addon.voice_messages.command.max_duration_warning",
                            duration.inWholeSeconds,
                            10,
                        )
                    }
                },
            )
        } catch (_: TimeoutCancellationException) {
            player.sendTranslatable(
                "pv.addon.voice_messages.command.recording_timeout",
                timeoutDuration.inWholeSeconds,
                10,
            )
            return null
        } catch (e: Throwable) {
            e.printStackTrace()
            return null
        }

    // todo: check min length?

    val voiceMessage = createVoiceMessage(voiceServer, encodedFrames)
    if (voiceMessage.duration == maxDuration) {
        player.sendTranslatable(
            "pv.addon.voice_messages.command.max_duration_reached",
            maxDuration.inWholeSeconds.padStartZero(),
        )
    }

    messageStore.save(voiceMessage)

    return voiceMessage
}

// private fun sendDirectVoiceMessageCommand(
//    sender: DirectMessageSender,
//    config: AddonConfig,
//    voiceServer: PlasmoVoiceServer,
//    voiceRecorder: VoiceActivationRecorder,
//    messageStore: VoiceMessageStore,
// ) = literalCommand("private") {
//    val target by argument("target", StringArgumentType.word()) {
//        suggests { _, suggestionsBuilder ->
//            CoroutineScope(Dispatchers.Default).future {
//                sender
//                    .getOnlinePlayers()
//                    .filter { it.startsWith(suggestionsBuilder.remainingLowerCase, ignoreCase = true) }
//                    .forEach { player -> suggestionsBuilder.suggest(player) }
//
//                suggestionsBuilder.build()
//            }
//        }
//    }
//
//    executesAsync { context ->
//        val player =
//            context.source.executor as? Player
//                ?: throw IllegalStateException("Player only command")
//
//        val voiceMessage = recordAndSaveVoiceMessage(player, config, voiceServer, voiceRecorder, messageStore) ?: return@executesAsync
//
//        sender.sendVoiceMessage(player, target, voiceMessage)
//    }
// }

private fun sendChatVoiceMessageCommand(
    chatName: String,
    chatSender: ChatMessageSender,
    config: AddonConfig,
    voiceServer: PlasmoVoiceServer,
    voiceRecorder: VoiceActivationRecorder,
    messageStore: VoiceMessageStore,
) = dslCommand(chatName) {
    executesCoroutine { context ->
        val player =
            context.source as? McServerPlayer
                ?: throw IllegalStateException("Player only command")

        if (!chatSender.canSendMessage(player)) return@executesCoroutine

        val voiceMessage = recordAndSaveVoiceMessage(player, config, voiceServer, voiceRecorder, messageStore) ?: return@executesCoroutine

        chatSender.sendVoiceMessage(player, voiceMessage)
    }
}

fun voiceMessageCommand(
    name: String,
    config: AddonConfig,
    voiceServer: PlasmoVoiceServer,
    voiceRecorder: VoiceActivationRecorder,
    messageStore: VoiceMessageStore,
): DslCommand =
    dslCommand(name) {
        command(
            sendChatVoiceMessageCommand(
                "global",
                DefaultGlobalMessageSender(voiceServer.minecraftServer),
                config,
                voiceServer,
                voiceRecorder,
                messageStore,
            ),
        )
    }
