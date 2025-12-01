package dev.apehum.voicemessages.api.command.dsl

import su.plo.slib.api.command.McCommandSource

/**
 * Context of the command execution.
 */
interface CommandContext {
    /**
     * Source that executes the command.
     */
    val source: McCommandSource

    /**
     * Gets parsed argument's value.
     *
     * @param name The name of the argument
     * @return An argument value.
     * @throws IllegalArgumentException if argument doesn't exist.
     */
    fun <T> getArgumentValue(name: String): T
}
