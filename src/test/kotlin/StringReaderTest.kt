import dev.apehum.voicemessages.command.dsl.argument.StringReader
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class StringReaderTest {
    @Test
    fun testEmpty() {
        val reader = StringReader("")
        assertEquals(false, reader.isReadable())
        assertThrows<IndexOutOfBoundsException> { reader.readString() }
        assertThrows<IndexOutOfBoundsException> { reader.read() }
    }

    @Test
    fun testReadString() {
        val reader = StringReader("hello world")
        assertEquals(true, reader.isReadable())
        reader.skipWhitespaces()
        assertEquals("hello", reader.readString())
        reader.skipWhitespaces()
        assertEquals("world", reader.readString())
        assertThrows<IndexOutOfBoundsException> { reader.readString() }
    }

    @Test
    fun testReadChar() {
        val reader = StringReader("hello")
        assertEquals(true, reader.isReadable())
        assertEquals('h', reader.read())
        assertEquals(4, reader.remaining())
    }
}
