package dev.apehum.voicemessages.playback

import su.plo.voice.api.server.PlasmoVoiceServer
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

/**
 * Creates a voice message from encoded audio frames.
 *
 * This function decodes the frames to generate a waveform visualization
 * and creates a [VoiceMessage] with a unique ID.
 *
 * @param voiceServer The PlasmoVoice server instance for decoding
 * @param encodedFrames List of Opus-encoded audio frames
 * @return A new [VoiceMessage] with generated waveform
 */
fun createVoiceMessage(
    voiceServer: PlasmoVoiceServer,
    encodedFrames: List<ByteArray>,
): VoiceMessage {
    val decodedFrames =
        voiceServer.createOpusDecoder(false).use { decoder ->
            encodedFrames.map { decoder.decode(it) }
        }
    val waveform = generateWaveform(decodedFrames, 16)

    return VoiceMessage(UUID.randomUUID(), encodedFrames, waveform)
}

/**
 * Represents a recorded voice message.
 *
 * @property id Unique identifier for the voice message
 * @property encodedFrames List of Opus-encoded audio frames (20ms each)
 * @property waveform Normalized waveform data for visualization (values between 0.0 and 1.0)
 * @property duration Total duration of the voice message, calculated as 20ms per frame
 */
data class VoiceMessage(
    val id: UUID,
    val encodedFrames: List<ByteArray>,
    val waveform: List<Double>,
) {
    val duration by lazy {
        (encodedFrames.size * 20L).milliseconds
    }
}
