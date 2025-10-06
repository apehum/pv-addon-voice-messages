package dev.apehum.voicemessages.command.dsl.argument

class StringArgument : CommandArgument<String> {
    override fun parse(reader: StringReader): String = reader.readString()
}
