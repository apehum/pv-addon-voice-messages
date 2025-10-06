package dev.apehum.voicemessages.command.dsl.argument

import su.plo.slib.api.command.McCommandSource

interface CommandArgument<T> {
    fun parse(reader: StringReader): T

    fun suggest(
        source: McCommandSource,
        arguments: Array<String>,
    ): List<String> = emptyList()
}
