package dev.apehum.voicemessages.playback

import su.plo.voice.api.util.AudioUtil
import kotlin.math.min

fun generateWaveform(
    samples: List<ShortArray>,
    desiredLength: Int,
): List<Double> {
    val sampleSize = samples.first().size

    val joinedSamples = ShortArray(samples.size * sampleSize)
    samples.forEachIndexed { index, currentSamples ->
        System.arraycopy(currentSamples, 0, joinedSamples, index * sampleSize, sampleSize)
    }

    val chunkSize = joinedSamples.size / desiredLength
    val audioLevels =
        (0 until desiredLength).map { chunk ->
            val offset = chunk * chunkSize

            AudioUtil.calculateAudioLevel(
                joinedSamples,
                offset,
                min(offset + chunkSize, joinedSamples.size),
            )
        }

    return audioLevels.map { 1.0 - (it / -127.0) }
}
