package dev.apehum.voicemessages.api.command.dsl.argument

import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.command.McCommandSource

/**
 * A command argument paired with the name it is bound to.
 *
 * @property name The argument name
 * @property argument The argument implementation
 * @property displayName Provides the name shown as the input placeholder in the client mod
 */
data class NamedCommandArgument<T>(
    val name: String,
    val argument: CommandArgument<T>,
    val displayName: DisplayNameProvider,
) {
    constructor(
        name: String,
        argument: CommandArgument<T>,
    ) : this(name, argument, { null })

    fun interface DisplayNameProvider {
        operator fun invoke(source: McCommandSource): McTextComponent?
    }
}

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
