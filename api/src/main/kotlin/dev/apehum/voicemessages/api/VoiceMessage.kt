package dev.apehum.voicemessages.api

import java.time.Duration
import java.util.UUID

private const val FRAME_TIME = 20L

/**
 * Represents a recorded voice message.
 *
 * @property id Unique identifier of the voice message
 * @property encodedFrames List of Opus-encoded audio frames (20ms each)
 * @property waveform Normalized waveform data for visualization (values between 0.0 and 1.0)
 * @property duration Total duration of the voice message, calculated as 20ms per frame
 */
data class VoiceMessage(
    val id: UUID,
    val encodedFrames: List<ByteArray>,
    val waveform: List<Double>,
) {
    val duration: Duration = Duration.ofMillis(encodedFrames.size * FRAME_TIME)
}
