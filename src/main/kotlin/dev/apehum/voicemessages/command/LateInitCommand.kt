package dev.apehum.voicemessages.command

import su.plo.slib.api.command.McCommand
import su.plo.slib.api.command.McCommandManager
import su.plo.slib.api.command.McCommandSource

class LateInitCommand(
    val name: String,
    private val aliases: List<String> = emptyList(),
) : McCommand {
    private lateinit var command: McCommand

    constructor(name: String, vararg aliases: String) : this(name, aliases.toList())

    fun register(commandManager: McCommandManager<McCommand>) {
        commandManager.register(name, this, *aliases.toTypedArray())
    }

    fun initialize(command: McCommand) {
        this.command = command
    }

    override fun execute(
        source: McCommandSource,
        arguments: Array<String>,
    ) {
        command.execute(source, arguments)
    }

    override fun suggest(
        source: McCommandSource,
        arguments: Array<String>,
    ): List<String> = command.suggest(source, arguments)

    override fun hasPermission(
        source: McCommandSource,
        arguments: Array<String>?,
    ): Boolean = command.hasPermission(source, arguments)
}
