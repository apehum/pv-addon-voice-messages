package dev.apehum.voicemessages.command.dsl.argument

class StringReader(
    private val input: String,
) {
    var readerIndex = 0

    fun remaining() = input.length - readerIndex

    fun isReadable(): Boolean = isReadable(1)

    fun isReadable(size: Int): Boolean = readerIndex + size <= input.length

    fun skipWhitespaces() {
        while (isReadable()) {
            val next = read()
            if (!next.isWhitespace()) {
                readerIndex--
                break
            }
        }
    }

    fun read(): Char {
        if (!isReadable()) {
            throw IndexOutOfBoundsException(readerIndex + 1)
        }

        return input[readerIndex++]
    }

    fun readString(): String {
        if (!isReadable()) return ""

        val start = readerIndex
        while (isReadable()) {
            val next = read()
            if (next.isWhitespace()) {
                readerIndex--
                break
            }
        }

        return input.substring(start, readerIndex)
    }
}
