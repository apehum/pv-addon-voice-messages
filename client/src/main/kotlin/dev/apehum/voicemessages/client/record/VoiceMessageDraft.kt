package dev.apehum.voicemessages.client.record

import dev.apehum.voicemessages.playback.WaveformKt
import su.plo.voice.api.audio.codec.AudioEncoder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

const val FRAME_DURATION_MS = 20L

private const val MIN_TRIM_FRAMES = 10

class VoiceMessageDraft(
    val frames: List<ShortArray>,
) {
    val duration: Duration = (frames.size * FRAME_DURATION_MS).milliseconds

    var trimStart: Int = 0
        private set

    var trimEnd: Int = frames.size
        private set

    val trimmedFrames: List<ShortArray>
        get() = frames.subList(trimStart, trimEnd)

    val trimmedDuration: Duration
        get() = ((trimEnd - trimStart) * FRAME_DURATION_MS).milliseconds

    private var waveformBars = 0
    private var waveformLevels = emptyList<Double>()

    fun trimStartTo(frame: Int) {
        trimStart = frame.coerceIn(0, (trimEnd - MIN_TRIM_FRAMES).coerceAtLeast(0))
    }

    fun trimEndTo(frame: Int) {
        trimEnd = frame.coerceIn((trimStart + MIN_TRIM_FRAMES).coerceAtMost(frames.size), frames.size)
    }

    fun waveform(bars: Int): List<Double> {
        if (bars == waveformBars) return waveformLevels

        waveformBars = bars
        waveformLevels = WaveformKt.generateWaveform(frames, bars.coerceIn(1, frames.size * frames.first().size))

        return waveformLevels
    }

    fun encode(encoder: AudioEncoder): List<ByteArray> = trimmedFrames.map(encoder::encode)
}
