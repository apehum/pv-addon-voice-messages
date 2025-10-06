package dev.apehum.voicemessages.command

import dev.apehum.voicemessages.command.dsl.dslCommand
import dev.apehum.voicemessages.playback.VoiceMessagePlayer
import dev.apehum.voicemessages.record.VoiceActivationRecorder
import dev.apehum.voicemessages.store.VoiceMessageStore
import dev.apehum.voicemessages.util.extension.sendTranslatable
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.chat.style.McTextClickEvent
import su.plo.slib.api.server.entity.player.McServerPlayer

private fun playVoiceMessageCommand(
    messageStore: VoiceMessageStore,
    messagePlayer: VoiceMessagePlayer,
) = dslCommand("play") {
    val id by argument("id", UUIDArgumentType())

    executesCoroutine { context ->
        val player = context.source as? McServerPlayer ?: return@executesCoroutine

        val voiceMessage =
            messageStore.getById(id) ?: run {
                player.sendTranslatable("pv.addon.voice_messages.command.message_not_found")
                return@executesCoroutine
            }

        messagePlayer.play(player, voiceMessage, true)

        player.sendTranslatable(
            "pv.addon.voice_messages.command.playback_started",
            McTextComponent
                .translatable("pv.addon.voice_messages.command.playback_cancel")
                .clickEvent(McTextClickEvent.runCommand("/vm-actions playback-cancel")),
        )
    }
}

// additional command for actions to avoid bloat in tab completion
fun voiceMessageActionsCommand(
    voiceRecorder: VoiceActivationRecorder,
    messageStore: VoiceMessageStore,
    messagePlayer: VoiceMessagePlayer,
) = dslCommand("vm-actions") {
    command(playVoiceMessageCommand(messageStore, messagePlayer))

    command("record-cancel") {
        executes { context ->
            val player = context.source as? McServerPlayer ?: throw IllegalStateException("Player only command")

            if (voiceRecorder.stop(player, Exception("Recording is cancelled"))) {
                player.sendTranslatable("pv.addon.voice_messages.command.recording_cancelled")
            } else {
                player.sendTranslatable("pv.addon.voice_messages.command.no_active_recording")
            }
        }
    }

    command("record-stop") {
        executes { context ->
            val player = context.source as? McServerPlayer ?: throw IllegalStateException("Player only command")

            if (!voiceRecorder.stop(player)) {
                player.sendTranslatable("pv.addon.voice_messages.command.no_active_recording")
            }
        }
    }

    command("playback-cancel") {
        executes { context ->
            val player = context.source as? McServerPlayer ?: throw IllegalStateException("Player only command")

            if (messagePlayer.cancel(player)) {
                player.sendTranslatable("pv.addon.voice_messages.command.playback_cancelled")
            } else {
                player.sendTranslatable("pv.addon.voice_messages.command.no_active_playback")
            }
        }
    }
}
