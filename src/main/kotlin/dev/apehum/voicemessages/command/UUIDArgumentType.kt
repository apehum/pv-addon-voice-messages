package dev.apehum.voicemessages.command

import dev.apehum.voicemessages.command.dsl.argument.CommandArgument
import dev.apehum.voicemessages.command.dsl.argument.StringReader
import java.util.UUID

class UUIDArgumentType : CommandArgument<UUID> {
    override fun parse(reader: StringReader): UUID {
        val string = reader.readString()
        return UUID.fromString(string)
    }
}
