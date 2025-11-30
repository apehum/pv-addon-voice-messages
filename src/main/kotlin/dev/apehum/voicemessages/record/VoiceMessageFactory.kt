package dev.apehum.voicemessages.record

import dev.apehum.voicemessages.api.VoiceMessage
import dev.apehum.voicemessages.playback.generateWaveform
import su.plo.voice.api.server.PlasmoVoiceServer
import java.util.UUID

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
