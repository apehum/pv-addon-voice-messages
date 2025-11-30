package dev.apehum.voicemessages.command

import dev.apehum.voicemessages.VoiceMessagesAddon
import dev.apehum.voicemessages.api.VoiceMessage
import dev.apehum.voicemessages.api.chat.ChatContext
import dev.apehum.voicemessages.api.chat.ChatMessageSender
import dev.apehum.voicemessages.api.chat.ChatMessageSenderRegistry
import dev.apehum.voicemessages.api.record.RecordingStopCause
import dev.apehum.voicemessages.api.record.RecordingTimeoutCause
import dev.apehum.voicemessages.api.storage.draft.VoiceMessageDraft
import dev.apehum.voicemessages.api.storage.draft.VoiceMessageDraftStorage
import dev.apehum.voicemessages.command.dsl.DslCommand
import dev.apehum.voicemessages.command.dsl.dslCommand
import dev.apehum.voicemessages.playback.component
import dev.apehum.voicemessages.record.VoiceMessageRecorder
import dev.apehum.voicemessages.util.extension.asVoicePlayer
import dev.apehum.voicemessages.util.extension.sendTranslatable
import kotlinx.coroutines.future.await
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.chat.style.McTextClickEvent
import su.plo.slib.api.server.entity.player.McServerPlayer
import su.plo.voice.api.server.PlasmoVoiceServer

private suspend fun recordAndSaveVoiceMessage(
    player: McServerPlayer,
    voiceMessageRecorder: VoiceMessageRecorder,
    draftStore: VoiceMessageDraftStorage,
    chatSenderName: String,
    chatContext: ChatContext,
): VoiceMessage? {
    val voiceMessage =
        try {
            voiceMessageRecorder.record(player)
        } catch (e: RecordingTimeoutCause) {
            player.sendTranslatable(
                "pv.addon.voice_messages.command.recording_timeout",
                e.timeout.seconds,
            )
            return null
        } catch (_: RecordingStopCause) {
            // we don't care about cause here
            return null
        } catch (e: Throwable) {
            VoiceMessagesAddon.logger.error("Failed to record voice message", e)
            return null
        }

    // todo: check min length?

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
    voiceServer: PlasmoVoiceServer,
    voiceMessageRecorder: VoiceMessageRecorder,
    draftStore: VoiceMessageDraftStorage,
) = dslCommand(chatSenderName) {
    chatSender
        .createArguments()
        .forEach { (name, argument) ->
            argument(name, argument)
        }

    checkPermission { context ->
        context.source.hasPermission("pv.addon.voice_messages.record.*") ||
            context.source.hasPermission("pv.addon.voice_messages.record.$chatSenderName")
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

        recordAndSaveVoiceMessage(
            player,
            voiceMessageRecorder,
            draftStore,
            chatSenderName,
            chatContext,
        )
    }
}

fun voiceMessageCommand(
    name: String,
    voiceServer: PlasmoVoiceServer,
    voiceMessageRecorder: VoiceMessageRecorder,
    draftStore: VoiceMessageDraftStorage,
    senderRegistry: ChatMessageSenderRegistry,
): DslCommand =
    dslCommand(name) {
        checkPermission { context ->
            context.source.hasPermission("pv.addon.voice_messages.record.*") ||
                senderRegistry.getSenders().map { it.key }.any {
                    context.source.hasPermission("pv.addon.voice_messages.record.$it")
                }
        }

        senderRegistry.getSenders().forEach { (senderName, sender) ->
            @Suppress("UNCHECKED_CAST")
            command(
                sendChatVoiceMessageCommand(
                    senderName,
                    sender as ChatMessageSender<ChatContext>,
                    voiceServer,
                    voiceMessageRecorder,
                    draftStore,
                ),
            )
        }

        redirect { arrayOf("default") }
    }
