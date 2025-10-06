import dev.apehum.voicemessages.playback.VoiceMessage
import dev.apehum.voicemessages.playback.ogg
import org.chenliang.oggus.ogg.OggStream
import org.chenliang.oggus.opus.CommentHeader
import org.chenliang.oggus.opus.IdHeader
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VoiceMessageOggTest {
    @Test
    fun testOggGeneration() {
        val encodedFrames =
            listOf(
                createMockOpusFrame(),
                createMockOpusFrame(),
                createMockOpusFrame(),
            )
        val voiceMessage =
            VoiceMessage(
                id = UUID.randomUUID(),
                encodedFrames = encodedFrames,
                waveform = emptyList(),
            )

        val oggBytes = voiceMessage.ogg()

        assertTrue(oggBytes.isNotEmpty(), "OGG file should not be empty")

        assertEquals('O'.code.toByte(), oggBytes[0])
        assertEquals('g'.code.toByte(), oggBytes[1])
        assertEquals('g'.code.toByte(), oggBytes[2])
        assertEquals('S'.code.toByte(), oggBytes[3])
    }

    @Test
    fun testOggStructure() {
        val encodedFrames =
            listOf(
                createMockOpusFrame(),
                createMockOpusFrame(),
            )
        val voiceMessage =
            VoiceMessage(
                id = UUID.randomUUID(),
                encodedFrames = encodedFrames,
                waveform = emptyList(),
            )

        val oggBytes = voiceMessage.ogg()
        val oggStream = OggStream.from(ByteArrayInputStream(oggBytes))

        val firstPage = oggStream.readPage()
        assertNotNull(firstPage, "First page should exist")
        assertTrue(firstPage.isBOS, "First page should be BOS")
        assertEquals(0, firstPage.seqNum, "First page sequence number should be 0")

        val idHeader = IdHeader.from(firstPage.dataPackets[0])
        assertEquals(1, idHeader.channelCount)
        assertEquals(48000, idHeader.inputSampleRate)
        assertEquals(312, idHeader.preSkip)

        val secondPage = oggStream.readPage()
        assertNotNull(secondPage, "Second page should exist")
        assertEquals(1, secondPage.seqNum, "Second page sequence number should be 1")

        val commentHeader = CommentHeader.from(secondPage.dataPackets[0])
        assertEquals("Plasmo Voice", commentHeader.vendor)

        val thirdPage = oggStream.readPage()
        assertNotNull(thirdPage, "Third page should exist")
        assertEquals(2, thirdPage.seqNum, "Third page sequence number should be 2")
        assertTrue(thirdPage.dataPackets.isNotEmpty(), "Should have audio data packets")
    }

    @Test
    fun testOggEOSFlag() {
        val encodedFrames = listOf(createMockOpusFrame())
        val voiceMessage =
            VoiceMessage(
                id = UUID.randomUUID(),
                encodedFrames = encodedFrames,
                waveform = listOf(0.5),
            )

        val oggBytes = voiceMessage.ogg()
        val oggStream = OggStream.from(ByteArrayInputStream(oggBytes))

        var lastPage = oggStream.readPage()
        var page = oggStream.readPage()

        while (page != null) {
            lastPage = page
            page = oggStream.readPage()
        }

        assertNotNull(lastPage, "Should have at least one page")
        assertTrue(lastPage.isEOS, "Last page should have EOS flag set")
    }

    @Test
    fun testGranulePosition() {
        val encodedFrames = List(10) { createMockOpusFrame() }
        val voiceMessage =
            VoiceMessage(
                id = UUID.randomUUID(),
                encodedFrames = encodedFrames,
                waveform = List(10) { 0.5 },
            )

        val oggBytes = voiceMessage.ogg()
        val oggStream = OggStream.from(ByteArrayInputStream(oggBytes))

        oggStream.readPage() // ID header
        oggStream.readPage() // Comment header

        var previousGranulePosition = 0L
        var page = oggStream.readPage()

        while (page != null) {
            assertTrue(
                page.granulePosition > previousGranulePosition,
                "Granule position should be monotonically increasing",
            )
            previousGranulePosition = page.granulePosition
            page = oggStream.readPage()
        }
    }

    @Test
    fun testMultipleFramesPaging() {
        val encodedFrames = List(100) { createMockOpusFrame() }
        val voiceMessage =
            VoiceMessage(
                id = UUID.randomUUID(),
                encodedFrames = encodedFrames,
                waveform = List(100) { 0.5 },
            )

        val oggBytes = voiceMessage.ogg()
        val oggStream = OggStream.from(ByteArrayInputStream(oggBytes))

        var pageCount = 0
        var page = oggStream.readPage()

        while (page != null) {
            pageCount++
            page = oggStream.readPage()
        }

        assertTrue(pageCount >= 3, "Should have multiple pages for 100 frames")
    }

    private fun createMockOpusFrame(): ByteArray {
        // TOC byte: config 12 (20ms SILK-only NB), stereo=0 (mono), code 0
        val toc = (12 shl 3) or 0
        // Add some dummy payload
        return byteArrayOf(toc.toByte(), 0x01, 0x02, 0x03, 0x04)
    }
}
