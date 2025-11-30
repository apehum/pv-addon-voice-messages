package dev.apehum.voicemessages.record

import dev.apehum.voicemessages.AddonConfig
import dev.apehum.voicemessages.api.VoiceMessage
import dev.apehum.voicemessages.api.record.NewRecordingStartCause
import dev.apehum.voicemessages.api.record.RecordingTimeoutCause
import dev.apehum.voicemessages.playback.minutesAndSeconds
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
import kotlin.time.toJavaDuration

class VoiceMessageRecorder(
    private val config: AddonConfig,
    private val voiceServer: PlasmoVoiceServer,
    private val voiceRecorder: VoiceActivationRecorder,
) {
    suspend fun record(player: McServerPlayer): VoiceMessage {
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
                                duration.minutesAndSeconds(),
                                maxDuration.minutesAndSeconds(),
                            )
                        }

                        if (!maxDurationWarningSent && maxDuration.inWholeSeconds - duration.inWholeSeconds <= 10) {
                            val remainingDuration = maxDuration.inWholeSeconds - duration.inWholeSeconds
                            maxDurationWarningSent = true
                            player.sendTranslatable(
                                "pv.addon.voice_messages.command.max_duration_warning",
                                duration.minutesAndSeconds(),
                                remainingDuration,
                            )
                        }
                    },
                )
            } catch (_: TimeoutCancellationException) {
                throw RecordingTimeoutCause(timeoutDuration.toJavaDuration())
            }

        return createVoiceMessage(voiceServer, encodedFrames)
    }
}
