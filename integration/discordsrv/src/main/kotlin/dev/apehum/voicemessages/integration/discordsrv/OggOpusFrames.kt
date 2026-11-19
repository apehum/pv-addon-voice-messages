package dev.apehum.voicemessages.integration.discordsrv

import org.chenliang.oggus.opus.OggOpusStream
import java.io.InputStream

private const val VOICE_MESSAGE_FRAME_SIZE_MS = 20.0

fun InputStream.readOpusFrames(): List<ByteArray> {
    val oggStream = OggOpusStream.from(this)
    val channelMappingFamily = oggStream.idHeader.channelMappingFamily
    require(channelMappingFamily == 0) { "Unsupported Opus channel mapping family $channelMappingFamily" }

    return generateSequence { oggStream.readAudioPacket() }
        .map { it.opusPackets.single() }
        .onEach {
            require(it.code == 0 && it.config.frameSize == VOICE_MESSAGE_FRAME_SIZE_MS) {
                "Unsupported Opus packet: code ${it.code}, frame size ${it.config.frameSize} ms"
            }
        }.map { it.dumpToStandardFormat() }
        .toList()
}
