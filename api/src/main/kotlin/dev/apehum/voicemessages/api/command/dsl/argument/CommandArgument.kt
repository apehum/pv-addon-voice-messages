package dev.apehum.voicemessages.api.command.dsl.argument

import su.plo.slib.api.command.McCommandSource

/**
 * A command argument paired with the name it is bound to.
 *
 * @property name The argument name
 * @property argument The argument implementation
 */
data class NamedCommandArgument<T>(
    val name: String,
    val argument: CommandArgument<T>,
)

/**
 * Represents a command argument used for parsing a value of type [T] from a [StringReader].
 */
interface CommandArgument<T> {
    /**
     * Parses a value of type [T] from the given [reader].
     *
     * @param source The command source executing the command
     * @param reader A string reader positioned at the argument location
     * @return The parsed value.
     */
    fun parse(
        source: McCommandSource,
        reader: StringReader,
    ): T

    /**
     * Provides suggestions for input completion.
     *
     * @param source The command source requesting suggestions
     * @param reader A string reader positioned at the argument location
     */
    fun suggest(
        source: McCommandSource,
        reader: StringReader,
    ): List<String> = emptyList()
}
