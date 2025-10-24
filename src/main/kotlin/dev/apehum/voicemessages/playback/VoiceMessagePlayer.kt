package dev.apehum.voicemessages.playback

import com.google.common.collect.Maps
import dev.apehum.voicemessages.util.extension.padStartZero
import dev.apehum.voicemessages.util.extension.startCancellable
import dev.apehum.voicemessages.util.extension.toLegacyString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.chat.style.McTextStyle
import su.plo.slib.api.server.entity.player.McServerPlayer
import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.api.server.audio.line.ServerSourceLine
import su.plo.voice.api.server.audio.provider.ArrayAudioFrameProvider
import su.plo.voice.api.server.audio.source.AudioSender
import su.plo.voice.api.server.audio.source.ServerDirectSource
import kotlin.jvm.optionals.getOrNull
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

class VoiceMessagePlayer(
    private val sourceLine: ServerSourceLine,
    private val voiceServer: PlasmoVoiceServer,
) {
    private val jobs: MutableMap<McServerPlayer, Job> = Maps.newConcurrentMap()

    fun clear() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }

    fun play(
        player: McServerPlayer,
        message: VoiceMessage,
        showWaveform: Boolean = false,
    ): Job {
        jobs.remove(player)?.cancel()

        return CoroutineScope(Dispatchers.Default)
            .launch {
                val voicePlayer =
                    voiceServer.playerManager
                        .getPlayerById(player.uuid)
                        .getOrNull() ?: return@launch

                val source = sourceLine.createDirectSource(voicePlayer)
                val audioSender = createVisualAudioSender(source, message, showWaveform)

                audioSender.startCancellable()
                jobs.remove(player)
            }.also { jobs[player] = it }
    }

    fun cancel(player: McServerPlayer): Boolean {
        val job = jobs.remove(player) ?: return false

        job.cancel()

        return true
    }

    private fun createVisualAudioSender(
        source: ServerDirectSource,
        message: VoiceMessage,
        showWaveform: Boolean = true,
    ): AudioSender {
        val waveformComponents = message.waveformComponents()

        val frameProvider = ArrayAudioFrameProvider(voiceServer, false)
        message.encodedFrames.forEach { packet ->
            frameProvider.addEncodedFrame(packet)
        }

        val totalDuration = (message.encodedFrames.size * 20L).milliseconds

        // delay sending source name to compensate jitter and audio buffer on the client
        // this is not perfect, but we can't know current jitter buffer on the client
        // this should be good enough
        var delayedSequenceNumber = -4L

        return AudioSender(
            frameProvider,
            { frame, sequenceNumber ->
                if (!source.sendAudioFrame(frame, sequenceNumber)) {
                    return@AudioSender false
                }

                val currentDuration = ((delayedSequenceNumber++).coerceAtLeast(0) * 20L).milliseconds

                val currentTime =
                    McTextComponent.literal(
                        "[${currentDuration.minutesAndSeconds()} / ${totalDuration.minutesAndSeconds()}]",
                    )

                val sourceName =
                    if (showWaveform) {
                        val progress = currentDuration.inWholeMilliseconds.toFloat() / totalDuration.inWholeMilliseconds
                        val currentWaveformIndex = (progress * waveformComponents.size).roundToInt()

                        waveformComponents.mapIndexed { index, component ->
                            if (index > currentWaveformIndex) {
                                component.withStyle(McTextStyle.DARK_GRAY)
                            } else {
                                component.withStyle(McTextStyle.WHITE)
                            }
                        }
                    } else {
                        listOf(McTextComponent.literal("Voice Message"))
                    }

                // sources doesn't directly support component
                // BUT we can use legacy section
                source.setName(
                    McTextComponent
                        .empty()
                        .append(currentTime)
                        .append(McTextComponent.literal(" "))
                        .append(sourceName)
                        .toLegacyString(),
                )

                true
            },
            { sequenceNumber ->
                source.sendAudioEnd(sequenceNumber)
            },
        )
    }
}
