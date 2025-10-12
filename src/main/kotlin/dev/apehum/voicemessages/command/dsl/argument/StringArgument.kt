package dev.apehum.voicemessages.command.dsl.argument

import su.plo.slib.api.command.McCommandSource

class StringArgument : CommandArgument<String> {
    override fun parse(
        source: McCommandSource,
        reader: StringReader,
    ): String = reader.readString()
}
