package dev.apehum.voicemessages.command.dsl.argument

import su.plo.slib.api.command.McCommandSource

data class NamedCommandArgument<T>(
    val name: String,
    val argument: CommandArgument<T>,
)

interface CommandArgument<T> {
    fun parse(reader: StringReader): T

    fun suggest(
        source: McCommandSource,
        reader: StringReader,
    ): List<String> = emptyList()
}
