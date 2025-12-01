package dev.apehum.voicemessages.api.command.dsl.argument

/**
 * A simple mutable reader for command input parsing.
 *
 * @property input The raw string being read.
 */
class StringReader(
    private val input: String,
) {
    /**
     * The current read position within [input].
     */
    var readerIndex: Int = 0

    /**
     * Returns the number of unread characters remaining.
     */
    fun remaining(): Int = input.length - readerIndex

    /**
     * Returns true if at least one character can be read.
     */
    fun isReadable(): Boolean = isReadable(1)

    /**
     * Returns true if at least [size] characters can be read without
     * exceeding the input length.
     */
    fun isReadable(size: Int): Boolean = readerIndex + size <= input.length

    /**
     * Advances the [readerIndex] past any whitespace characters.
     *
     * Stops at the first non-whitespace character and leaves the reader
     * positioned on it.
     */
    fun skipWhitespaces() {
        while (isReadable()) {
            val next = read()
            if (!next.isWhitespace()) {
                readerIndex--
                break
            }
        }
    }

    /**
     * Reads and returns the next character.
     *
     * @throws IndexOutOfBoundsException if no more input is available.
     */
    fun read(): Char {
        if (!isReadable()) {
            throw IndexOutOfBoundsException(readerIndex + 1)
        }
        return input[readerIndex++]
    }

    /**
     * Reads a whitespace-delimited string starting at the current [readerIndex].
     *
     * Stops before the next whitespace and returns the substring consumed.
     *
     * Returns empty string if [isReadable] is false.
     */
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
