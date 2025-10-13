package dev.apehum.voicemessages.command.dsl

import dev.apehum.voicemessages.command.dsl.argument.CommandArgument
import dev.apehum.voicemessages.command.dsl.argument.StringReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import su.plo.slib.api.command.McCommand
import su.plo.slib.api.command.McCommandSource
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

typealias DslCommandRunnable = (context: DslCommandContext) -> Unit

typealias DslCommandCoroutineRunnable = suspend (context: DslCommandContext) -> Unit

private val currentContext = ThreadLocal<DslCommandContext>()

class DslCommandArgument<T>(
    val name: String,
    val inner: CommandArgument<T>,
) {
    operator fun provideDelegate(
        thisRef: Any?,
        property: KProperty<*>,
    ): ReadOnlyProperty<Any?, T> =
        ReadOnlyProperty { _, _ ->
            currentContext.get().getArgumentValue(name)
        }
}

data class DslParsedCommandArgument<T>(
    val name: String,
    val inner: CommandArgument<*>,
    val value: T,
)

data class DslCommandContext(
    val source: McCommandSource,
    val arguments: Map<String, DslParsedCommandArgument<*>>,
) {
    @Suppress("UNCHECKED_CAST")
    fun <T> getArgumentValue(name: String): T {
        if (!arguments.containsKey(name)) throw IllegalArgumentException("Argument[$name] not found")

        return arguments[name]?.value as T
    }

    operator fun <T> DslCommandArgument<T>.getValue(
        thisRef: Nothing?,
        property: KProperty<*>,
    ): T = getArgumentValue(name)
}

fun dslCommand(
    name: String,
    builder: DslCommandBuilder.() -> Unit = {},
): DslCommand = DslCommandBuilder(name).apply(builder).build()

class DslCommandBuilder(
    val name: String,
) {
    private val commands: MutableList<DslCommand> = mutableListOf()
    private val arguments: MutableList<DslCommandArgument<*>> = mutableListOf()

    private var executes: DslCommandRunnable? = null
    private var executesCoroutine: DslCommandCoroutineRunnable? = null
    private var coroutineScope: CoroutineScope? = null

    fun command(
        name: String,
        builder: DslCommandBuilder.() -> Unit,
    ) {
        if (arguments.isNotEmpty()) {
            throw IllegalArgumentException("Commands with arguments at the same time are not allowed")
        }

        val command = DslCommandBuilder(name).apply(builder)
        commands.add(command.build())
    }

    fun command(command: DslCommand) {
        if (arguments.isNotEmpty()) {
            throw IllegalArgumentException("Commands with arguments at the same time are not allowed")
        }

        commands.add(command)
    }

    fun <T : Any> argument(
        name: String,
        type: CommandArgument<T>,
    ): DslCommandArgument<T> {
        if (commands.isNotEmpty()) {
            throw IllegalArgumentException("Arguments with commands at the same time are not allowed")
        }

        return DslCommandArgument(name, type)
            .also { arguments.add(it) }
    }

    fun executes(runnable: DslCommandRunnable) {
        executes = runnable
    }

    fun executesCoroutine(
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
        runnable: DslCommandCoroutineRunnable,
    ) {
        executesCoroutine = runnable
        coroutineScope = scope
    }

    fun build(): DslCommand =
        DslCommand(
            name,
            commands,
            arguments,
            executes,
            executesCoroutine,
            coroutineScope,
        )
}

class DslCommand(
    val name: String,
    private val commands: List<DslCommand> = mutableListOf(),
    private val arguments: List<DslCommandArgument<*>> = mutableListOf(),
    private val executes: DslCommandRunnable? = null,
    private val executesCoroutine: DslCommandCoroutineRunnable? = null,
    private val coroutineScope: CoroutineScope? = null,
) : McCommand {
    override fun execute(
        source: McCommandSource,
        arguments: Array<String>,
    ) {
        if (commands.isNotEmpty()) {
            val argument =
                arguments.getOrNull(0) ?: run {
                    if (executes != null || executesCoroutine != null) {
                        execute(DslCommandContext(source, emptyMap()))
                        return
                    }

                    source.sendMessage("Usage: /$name <${commands.map { it.name }}>")
                    return
                }

            val command =
                commands.find { it.name == argument } ?: run {
                    source.sendMessage("$argument not found. Usage: /$name <${commands.map { it.name }}>")
                    return
                }
            val newArguments = arguments.takeIf { it.size > 1 }?.copyOfRange(1, arguments.size) ?: emptyArray()
            command.execute(source, newArguments)
            return
        }

        val argumentsReader = StringReader(arguments.joinToString(" "))

        val parsedArguments =
            this.arguments
                .map { argument ->
                    argumentsReader.skipWhitespaces()

                    val parsed =
                        try {
                            argument.inner.parse(source, argumentsReader)
                        } catch (e: Throwable) {
                            // todo: parse exception with friendly message
                            source.sendMessage("Failed to parse argument ${argument.name}: ${e.message}")
                        }

                    DslParsedCommandArgument(
                        argument.name,
                        argument.inner,
                        parsed,
                    )
                }.associateBy { it.name }

        val context = DslCommandContext(source, parsedArguments)
        execute(context)
    }

    private fun execute(context: DslCommandContext) {
        if (executes != null) {
            currentContext.set(context)
            executes.invoke(context)
            currentContext.remove()
        } else if (executesCoroutine != null && coroutineScope != null) {
            coroutineScope.launch {
                currentContext.set(context)
                executesCoroutine.invoke(context)
                currentContext.remove()
            }
        }
    }

    override fun suggest(
        source: McCommandSource,
        arguments: Array<String>,
    ): List<String> {
        if (commands.isNotEmpty()) {
            val argument =
                arguments.getOrNull(0) ?: run {
                    return commands.map { it.name }
                }

            val command =
                commands.find { it.name == argument } ?: run {
                    return commands
                        .map { it.name }
                        .filter { it.startsWith(argument, ignoreCase = true) }
                }
            val newArguments = arguments.takeIf { it.size > 1 }?.copyOfRange(1, arguments.size) ?: emptyArray()
            return command.suggest(source, newArguments)
        }

        val argumentIndex = arguments.size - 1
        val argument = this.arguments.getOrNull(argumentIndex) ?: return emptyList()

        val argumentsReader =
            StringReader(
                arguments.copyOfRange(argumentIndex, arguments.size).joinToString(" "),
            )

        return argument.inner.suggest(source, argumentsReader)
    }
}
