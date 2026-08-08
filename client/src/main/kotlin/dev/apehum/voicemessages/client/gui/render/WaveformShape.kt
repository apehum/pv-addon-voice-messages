package dev.apehum.voicemessages.client.gui.render

import kotlin.random.Random

const val WAVEFORM_SEGMENTS = 8

private const val MIN_RADIUS = 0.6f

private const val IDLE_SECONDS = 1.2f
private const val SPEAKING_SECONDS = 0.18f
private const val SPREAD = 0.35f

class WaveformShape(
    private val random: Random = Random.Default,
) {
    val radii = FloatArray(WAVEFORM_SEGMENTS) { randomRadius() }

    private val from = FloatArray(WAVEFORM_SEGMENTS) { radii[it] }
    private val to = FloatArray(WAVEFORM_SEGMENTS) { randomRadius() }
    private val progress = FloatArray(WAVEFORM_SEGMENTS)
    private val spread = FloatArray(WAVEFORM_SEGMENTS) { randomSpread() }

    fun advance(
        amplitude: Float,
        delta: Float,
    ) {
        val seconds = IDLE_SECONDS + (SPEAKING_SECONDS - IDLE_SECONDS) * amplitude.coerceIn(0f, 1f)

        for (segment in radii.indices) {
            progress[segment] += delta / (seconds * spread[segment])

            if (progress[segment] >= 1f) {
                progress[segment] = 0f
                from[segment] = to[segment]
                to[segment] = randomRadius()
                spread[segment] = randomSpread()
            }

            radii[segment] = from[segment] + (to[segment] - from[segment]) * progress[segment]
        }
    }

    private fun randomRadius(): Float = MIN_RADIUS + random.nextFloat() * (1f - MIN_RADIUS)

    private fun randomSpread(): Float = 1f + (random.nextFloat() * 2f - 1f) * SPREAD
}
