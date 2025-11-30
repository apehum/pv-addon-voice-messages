package dev.apehum.voicemessages.playback

import dev.apehum.voicemessages.api.VoiceMessage
import org.chenliang.oggus.ogg.OggPage
import org.chenliang.oggus.opus.CommentHeader
import org.chenliang.oggus.opus.IdHeader
import java.io.ByteArrayOutputStream

fun VoiceMessage.ogg(): ByteArray {
    var sequenceNumber = 0L
    val idHeader = createOggPage(0, sequenceNumber++, createIdHeader().dump())
    idHeader.setBOS()

    val commentHeader = createOggPage(0, sequenceNumber++, createCommentHeader().dump())

    val frameSize = 48000 / 1000 * 20

    val oggByteStream = ByteArrayOutputStream()
    oggByteStream.writeBytes(idHeader.dump())
    oggByteStream.writeBytes(commentHeader.dump())

    var granulePosition = 0L

    val pages = oggPages()
    pages.forEachIndexed { index, oggPage ->
        granulePosition += frameSize * oggPage.dataPackets.size
        oggPage.granulePosition = granulePosition
        oggPage.seqNum = sequenceNumber++
        oggPage.serialNum = 1
        if (index == pages.size - 1) oggPage.setEOS()

        oggByteStream.writeBytes(oggPage.dump())
    }

    return oggByteStream.toByteArray()
}

private fun VoiceMessage.oggPages(): List<OggPage> {
    val pages = mutableListOf<OggPage>(OggPage.empty())
    val frames = ArrayDeque(encodedFrames)

    while (frames.isNotEmpty()) {
        val frame = frames.removeFirst()
        val page = pages.last()
        page.addDataPacket(frame)

        if (frames.isNotEmpty() && page.dataPackets.size == 50) {
            pages.add(OggPage.empty())
        }
    }

    return pages
}

private fun createIdHeader(): IdHeader {
    val idHeader = IdHeader.emptyHeader()
    idHeader.majorVersion = 0
    idHeader.minorVersion = 1
    idHeader.channelCount = 1
    idHeader.preSkip = 312
    idHeader.inputSampleRate = 48000
    idHeader.outputGain = 0.0
    idHeader.channelMappingFamily = 0
    return idHeader
}

private fun createCommentHeader(): CommentHeader {
    val commentHeader = CommentHeader.emptyHeader()
    commentHeader.vendor = "Plasmo Voice"
    return commentHeader
}

private fun createOggPage(
    granulePosition: Long,
    seqNum: Long,
    vararg dataPackets: ByteArray,
): OggPage {
    val oggPage = createOggPage(granulePosition, seqNum)
    for (dataPacket in dataPackets) {
        oggPage.addDataPacket(dataPacket)
    }
    return oggPage
}

private fun createOggPage(
    granulePosition: Long,
    seqNum: Long,
): OggPage {
    val oggPage = OggPage.empty()
    oggPage.granulePosition = granulePosition
    oggPage.serialNum = 1
    oggPage.seqNum = seqNum
    return oggPage
}
