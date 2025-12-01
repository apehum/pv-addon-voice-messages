package dev.apehum.voicemessages.api.command.dsl.argument

import su.plo.slib.api.command.McCommandSource

/**
 * A command argument that reads a single whitespace-delimited string.
 */
class StringArgument : CommandArgument<String> {
    override fun parse(
        source: McCommandSource,
        reader: StringReader,
    ): String = reader.readString()
}
