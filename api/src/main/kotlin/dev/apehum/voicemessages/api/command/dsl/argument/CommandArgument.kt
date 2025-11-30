package dev.apehum.voicemessages.api.command.dsl.argument

import su.plo.slib.api.command.McCommandSource

data class NamedCommandArgument<T>(
    val name: String,
    val argument: CommandArgument<T>,
)

interface CommandArgument<T> {
    fun parse(
        source: McCommandSource,
        reader: StringReader,
    ): T

    fun suggest(
        source: McCommandSource,
        reader: StringReader,
    ): List<String> = emptyList()
}
