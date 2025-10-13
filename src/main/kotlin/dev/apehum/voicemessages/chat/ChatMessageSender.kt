package dev.apehum.voicemessages.chat

import dev.apehum.voicemessages.command.dsl.DslCommandContext
import dev.apehum.voicemessages.command.dsl.argument.NamedCommandArgument
import dev.apehum.voicemessages.playback.VoiceMessage
import java.util.concurrent.CompletableFuture

interface ChatMessageSender<T : ChatContext> {
    suspend fun sendVoiceMessage(
        context: T,
        message: VoiceMessage,
    )

    fun canSendMessage(context: T): Boolean

    fun createContext(context: DslCommandContext): CompletableFuture<T>

    fun createArguments(): List<NamedCommandArgument<out Any>> = emptyList()
}
