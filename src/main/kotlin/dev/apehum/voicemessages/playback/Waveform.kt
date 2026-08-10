package dev.apehum.voicemessages.playback

import su.plo.voice.api.util.AudioUtil

fun generateWaveform(
    samples: List<ShortArray>,
    desiredLength: Int,
): List<Double> {
    val sampleSize = samples.first().size
    val totalSamples = samples.size * sampleSize

    return (0 until desiredLength).map { chunk ->
        val start = (chunk.toLong() * totalSamples / desiredLength).toInt()
        val end = ((chunk + 1).toLong() * totalSamples / desiredLength).toInt()

        val firstFrame = start / sampleSize
        val lastFrame = (end - 1) / sampleSize

        val sumOfSquares =
            (firstFrame..lastFrame).sumOf { frameIndex ->
                val frame = samples[frameIndex]
                val frameStart = maxOf(start - frameIndex * sampleSize, 0)
                val frameEnd = minOf(end - frameIndex * sampleSize, sampleSize)

                (frameStart until frameEnd).sumOf { index ->
                    val sample = frame[index] / Short.MAX_VALUE.toDouble()
                    sample * sample
                }
            }

        AudioUtil.audioLevelToDoubleRange(
            AudioUtil.calculateAudioLevelFromRMS(sumOfSquares, end - start),
        )
    }
}
