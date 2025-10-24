package dev.apehum.voicemessages.command

import dev.apehum.voicemessages.AddonConfig
import dev.apehum.voicemessages.chat.ChatContext
import dev.apehum.voicemessages.chat.ChatMessageSender
import dev.apehum.voicemessages.chat.ChatMessageSenderRegistry
import dev.apehum.voicemessages.command.dsl.DslCommand
import dev.apehum.voicemessages.command.dsl.dslCommand
import dev.apehum.voicemessages.playback.VoiceMessage
import dev.apehum.voicemessages.playback.component
import dev.apehum.voicemessages.playback.createVoiceMessage
import dev.apehum.voicemessages.record.NewRecordingStartCause
import dev.apehum.voicemessages.record.RecordingStopCause
import dev.apehum.voicemessages.record.VoiceActivationRecorder
import dev.apehum.voicemessages.storage.draft.VoiceMessageDraft
import dev.apehum.voicemessages.storage.draft.VoiceMessageDraftStorage
import dev.apehum.voicemessages.util.extension.asVoicePlayer
import dev.apehum.voicemessages.util.extension.padStartZero
import dev.apehum.voicemessages.util.extension.sendTranslatable
import dev.apehum.voicemessages.util.extension.sendTranslatableActionbar
import dev.apehum.voicemessages.util.extension.translateClientPV
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.future.await
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
    draftStore: VoiceMessageDraftStorage,
    chatSenderName: String,
    chatContext: ChatContext,
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
            voiceRecorder.stop(player, NewRecordingStartCause())
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
                                .translatable("pv.addon.voice_messages.command.stop_recording")
                                .clickEvent(McTextClickEvent.runCommand("/vm-actions record-stop")),
                            McTextComponent
                                .translatable("pv.addon.voice_messages.command.cancel_and_delete_recording")
                                .clickEvent(McTextClickEvent.runCommand("/vm-actions record-cancel")),
                        )
                    }

                    if (config.actionbarWhenRecording && (isFirstFrame || duration.inWholeMilliseconds % 1000L == 0L)) {
                        player.sendTranslatableActionbar(
                            "pv.addon.voice_messages.command.recording_actionbar",
                            duration.inWholeSeconds.padStartZero(),
                            maxDuration.inWholeSeconds.padStartZero(),
                        )
                    }

                    if (!maxDurationWarningSent && maxDuration.inWholeSeconds - duration.inWholeSeconds <= 10) {
                        val remainingDuration = maxDuration.inWholeSeconds - duration.inWholeSeconds
                        maxDurationWarningSent = true
                        player.sendTranslatable(
                            "pv.addon.voice_messages.command.max_duration_warning",
                            duration.inWholeSeconds,
                            remainingDuration,
                        )
                    }
                },
            )
        } catch (_: RecordingStopCause) {
            // we don't care about cause here
            return null
        } catch (_: TimeoutCancellationException) {
            player.sendTranslatable(
                "pv.addon.voice_messages.command.recording_timeout",
                timeoutDuration.inWholeSeconds,
            )
            return null
        } catch (e: Throwable) {
            e.printStackTrace()
            return null
        }

    // todo: check min length?

    val voiceMessage = createVoiceMessage(voiceServer, encodedFrames)

    draftStore.save(player.uuid, VoiceMessageDraft(voiceMessage, chatSenderName, chatContext))

    player.sendTranslatable(
        "pv.addon.voice_messages.command.draft_saved",
        voiceMessage.component(
            McTextClickEvent.runCommand("/vm-actions draft-listen"),
        ),
        McTextComponent
            .translatable("pv.addon.voice_messages.command.send_draft")
            .clickEvent(McTextClickEvent.runCommand("/vm-actions draft-send")),
    )

    return voiceMessage
}

private fun sendChatVoiceMessageCommand(
    chatSenderName: String,
    chatSender: ChatMessageSender<ChatContext>,
    config: AddonConfig,
    voiceServer: PlasmoVoiceServer,
    voiceRecorder: VoiceActivationRecorder,
    draftStore: VoiceMessageDraftStorage,
) = dslCommand(chatSenderName) {
    chatSender
        .createArguments()
        .forEach { (name, argument) ->
            argument(name, argument)
        }

    executesCoroutine { context ->
        val player =
            context.source as? McServerPlayer
                ?: throw IllegalStateException("Player only command")

        val voicePlayer = player.asVoicePlayer(voiceServer)
        if (!voicePlayer.hasVoiceChat()) {
            player.sendTranslatable("pv.addon.voice_messages.command.pv_not_installed")
            return@executesCoroutine
        }

        val chatContext = chatSender.createContext(context).await()

        if (!chatSender.canSendMessage(chatContext)) return@executesCoroutine

        recordAndSaveVoiceMessage(player, config, voiceServer, voiceRecorder, draftStore, chatSenderName, chatContext)
    }
}

fun voiceMessageCommand(
    name: String,
    config: AddonConfig,
    voiceServer: PlasmoVoiceServer,
    voiceRecorder: VoiceActivationRecorder,
    draftStore: VoiceMessageDraftStorage,
    senderRegistry: ChatMessageSenderRegistry,
): DslCommand =
    dslCommand(name) {
        senderRegistry.getSenders().forEach { (senderName, sender) ->
            @Suppress("UNCHECKED_CAST")
            command(
                sendChatVoiceMessageCommand(
                    senderName,
                    sender as ChatMessageSender<ChatContext>,
                    config,
                    voiceServer,
                    voiceRecorder,
                    draftStore,
                ),
            )
        }

        executesCoroutine { context ->
            @Suppress("UNCHECKED_CAST")
            val chatSender = senderRegistry.getSender("default") as? ChatMessageSender<ChatContext> ?: return@executesCoroutine

            val player =
                context.source as? McServerPlayer
                    ?: throw IllegalStateException("Player only command")

            val voicePlayer = player.asVoicePlayer(voiceServer)
            if (!voicePlayer.hasVoiceChat()) {
                player.sendTranslatable("pv.addon.voice_messages.command.pv_not_installed")
                return@executesCoroutine
            }

            val chatContext = chatSender.createContext(context).await()

            if (!chatSender.canSendMessage(chatContext)) return@executesCoroutine

            recordAndSaveVoiceMessage(player, config, voiceServer, voiceRecorder, draftStore, "default", chatContext)
        }
    }
