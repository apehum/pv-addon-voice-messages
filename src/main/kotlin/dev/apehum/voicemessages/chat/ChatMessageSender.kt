package dev.apehum.voicemessages.chat

import dev.apehum.voicemessages.command.dsl.DslCommandContext
import dev.apehum.voicemessages.command.dsl.argument.NamedCommandArgument
import dev.apehum.voicemessages.playback.VoiceMessage

interface ChatMessageSender<T : ChatContext> {
    suspend fun sendVoiceMessage(
        context: T,
        message: VoiceMessage,
    )

    suspend fun canSendMessage(context: T): Boolean

    suspend fun createContext(context: DslCommandContext): T

    fun createArguments(): List<NamedCommandArgument<out Any>> = emptyList()
}
