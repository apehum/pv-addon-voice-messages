package dev.apehum.voicemessages.command.dsl

import dev.apehum.voicemessages.api.command.dsl.argument.NamedCommandArgument
import dev.apehum.voicemessages.api.command.dsl.argument.StringReader
import su.plo.slib.api.command.McCommandSource

sealed interface ArgumentsParseResult {
    data class Parsed(
        val context: DslCommandContext,
    ) : ArgumentsParseResult

    data class Failed(
        val argumentName: String,
        val message: String?,
    ) : ArgumentsParseResult
}

data class ArgumentSuggestions(
    val replaceFrom: Int,
    val suggestions: List<String>,
)

fun parseArguments(
    source: McCommandSource,
    arguments: List<NamedCommandArgument<*>>,
    input: String,
): ArgumentsParseResult {
    val reader = StringReader(input)

    val parsed =
        arguments
            .map { (name, argument) ->
                reader.skipWhitespaces()

                val value =
                    try {
                        argument.parse(source, reader)
                    } catch (e: Throwable) {
                        return ArgumentsParseResult.Failed(name, e.message)
                    }

                DslParsedCommandArgument(name, argument, value)
            }.associateBy { it.name }

    return ArgumentsParseResult.Parsed(DslCommandContext(source, parsed))
}

fun suggestArguments(
    source: McCommandSource,
    arguments: List<NamedCommandArgument<*>>,
    input: String,
): ArgumentSuggestions {
    val reader = StringReader(input)

    for ((_, argument) in arguments) {
        reader.skipWhitespaces()
        val start = reader.readerIndex

        val parsed = runCatching { argument.parse(source, reader) }.isSuccess

        if (!parsed || !reader.isReadable()) {
            reader.readerIndex = start
            return ArgumentSuggestions(start, argument.suggest(source, reader))
        }
    }

    return ArgumentSuggestions(input.length, emptyList())
}
