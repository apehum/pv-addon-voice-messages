package dev.apehum.voicemessages.command.dsl

import dev.apehum.voicemessages.api.command.dsl.CommandContext
import dev.apehum.voicemessages.api.command.dsl.argument.CommandArgument
import dev.apehum.voicemessages.api.command.dsl.argument.StringReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.launch
import su.plo.slib.api.command.McCommand
import su.plo.slib.api.command.McCommandSource
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun dslCommand(
    name: String,
    builder: DslCommandBuilder.() -> Unit = {},
): DslCommand = DslCommandBuilder(name).apply(builder).build()

typealias DslCommandRunnable = (context: DslCommandContext) -> Unit

typealias DslCommandCoroutineRunnable = suspend (context: DslCommandContext) -> Unit

typealias DslCommandRedirect = (source: McCommandSource) -> Array<String>

typealias DslCommandPermissionCheck = (context: DslCommandContext) -> Boolean

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
    override val source: McCommandSource,
    val arguments: Map<String, DslParsedCommandArgument<*>>,
) : CommandContext {
    @Suppress("UNCHECKED_CAST")
    override fun <T> getArgumentValue(name: String): T {
        if (!arguments.containsKey(name)) throw IllegalArgumentException("Argument[$name] not found")

        return arguments[name]?.value as T
    }

    operator fun <T> DslCommandArgument<T>.getValue(
        thisRef: Nothing?,
        property: KProperty<*>,
    ): T = getArgumentValue(name)
}

class DslCommandBuilder(
    val name: String,
) {
    private val commands: MutableList<DslCommand> = mutableListOf()
    private val arguments: MutableList<DslCommandArgument<*>> = mutableListOf()

    private var executes: DslCommandRunnable? = null
    private var executesCoroutine: DslCommandCoroutineRunnable? = null
    private var coroutineScope: CoroutineScope? = null
    private var permissionCheck: DslCommandPermissionCheck? = null
    private var redirect: DslCommandRedirect? = null

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

    fun checkPermission(permissionCheck: DslCommandPermissionCheck) {
        this@DslCommandBuilder.permissionCheck = permissionCheck
    }

    fun redirect(redirect: DslCommandRedirect) {
        this@DslCommandBuilder.redirect = redirect
    }

    fun executes(runnable: DslCommandRunnable) {
        executes = runnable
    }

    fun executesCoroutine(
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
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
            permissionCheck,
            redirect,
        )
}

class DslCommand(
    val name: String,
    private val commands: List<DslCommand> = mutableListOf(),
    private val arguments: List<DslCommandArgument<*>> = mutableListOf(),
    private val executes: DslCommandRunnable?,
    private val executesCoroutine: DslCommandCoroutineRunnable?,
    private val coroutineScope: CoroutineScope?,
    private val permissionCheck: DslCommandPermissionCheck?,
    private val redirect: DslCommandRedirect?,
) : McCommand {
    override fun execute(
        source: McCommandSource,
        arguments: Array<String>,
    ) {
        if (redirect != null && arguments.isEmpty()) {
            val newArguments = redirect(source)
            if (newArguments.isNotEmpty()) {
                return execute(source, newArguments)
            }
        }

        if (commands.isNotEmpty()) {
            val newArguments = arguments.takeIf { it.size > 1 }?.copyOfRange(1, arguments.size) ?: emptyArray()
            val availableCommands = commands.filter { it.hasPermission(source, newArguments) }

            val argument =
                arguments.getOrNull(0) ?: run {
                    if (executes != null || executesCoroutine != null) {
                        execute(DslCommandContext(source, emptyMap()))
                        return
                    }

                    source.sendMessage("Usage: /$name <${availableCommands.map { it.name }}>")
                    return
                }

            val command =
                availableCommands.find { it.name == argument } ?: run {
                    source.sendMessage("$argument not found. Usage: /$name <${availableCommands.map { it.name }}>")
                    return
                }
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
            coroutineScope.launch(currentContext.asContextElement(context)) {
                executesCoroutine.invoke(context)
            }
        }
    }

    override fun suggest(
        source: McCommandSource,
        arguments: Array<String>,
    ): List<String> {
        if (redirect != null && arguments.isEmpty()) {
            val newArguments = redirect(source)
            if (newArguments.isNotEmpty()) {
                return suggest(source, newArguments)
            }
        }

        if (commands.isNotEmpty()) {
            val newArguments = arguments.takeIf { it.size > 1 }?.copyOfRange(1, arguments.size) ?: emptyArray()
            val availableCommands = commands.filter { it.hasPermission(source, newArguments) }

            val argument =
                arguments.getOrNull(0) ?: run {
                    return availableCommands.map { it.name }
                }

            val command =
                availableCommands.find { it.name == argument } ?: run {
                    return availableCommands
                        .map { it.name }
                        .filter { it.startsWith(argument, ignoreCase = true) }
                }
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

    override fun hasPermission(
        source: McCommandSource,
        arguments: Array<String>?,
    ): Boolean {
        val hasRootPermission =
            if (permissionCheck != null) {
                permissionCheck(DslCommandContext(source, emptyMap()))
            } else {
                super.hasPermission(source, arguments)
            }

        if (!hasRootPermission) return false

        if (redirect != null && (arguments != null && arguments.isEmpty())) {
            val newArguments = redirect(source)
            if (newArguments.isNotEmpty()) {
                return hasPermission(source, newArguments)
            }
        }

        if (commands.isNotEmpty()) {
            val argument =
                arguments?.getOrNull(0) ?: run {
                    return true
                }

            val command =
                commands.find { it.name == argument } ?: run {
                    return true
                }
            val newArguments = arguments.takeIf { it.size > 1 }?.copyOfRange(1, arguments.size) ?: emptyArray()
            return command.hasPermission(source, newArguments)
        }

        // todo: arguments permissions

        return super.hasPermission(source, arguments)
    }
}
