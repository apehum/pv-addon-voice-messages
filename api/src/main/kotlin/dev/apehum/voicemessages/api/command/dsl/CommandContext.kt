package dev.apehum.voicemessages.api.command.dsl

import su.plo.slib.api.command.McCommandSource

interface CommandContext {
    val source: McCommandSource

    fun <T> getArgumentValue(name: String): T
}
