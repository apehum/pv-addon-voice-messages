package dev.apehum.voicemessages.playback

import su.plo.voice.api.server.PlasmoVoiceServer
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

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

data class VoiceMessage(
    val id: UUID,
    val encodedFrames: List<ByteArray>,
    val waveform: List<Double>,
) {
    val duration by lazy {
        (encodedFrames.size * 20L).milliseconds
    }
}
