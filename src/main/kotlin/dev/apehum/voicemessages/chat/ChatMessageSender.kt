package dev.apehum.voicemessages.chat

import dev.apehum.voicemessages.command.dsl.DslCommandContext
import dev.apehum.voicemessages.command.dsl.argument.NamedCommandArgument
import dev.apehum.voicemessages.playback.VoiceMessage
import java.util.concurrent.CompletableFuture

/**
 * Interface for sending voice messages in different chat contexts.
 *
 * Implementations define how voice messages are delivered (e.g., broadcast, direct message)
 * and what context information is required for sending.
 *
 * @param T The type of [ChatContext] this sender operates with
 */
interface ChatMessageSender<T : ChatContext> {
    /**
     * Sends a voice message in the specified context.
     *
     * @param context The chat context containing delivery information
     * @param message The voice message to send
     */
    suspend fun sendVoiceMessage(
        context: T,
        message: VoiceMessage,
    )

    /**
     * Checks if a message can be sent in the given context.
     *
     * @param context The chat context to validate
     * @return true if the message can be sent, false otherwise
     */
    fun canSendMessage(context: T): Boolean

    /**
     * Creates a chat context from a command context.
     *
     * @param context The command context to extract information from
     * @return A future that completes with the created chat context
     */
    fun createContext(context: DslCommandContext): CompletableFuture<T>

    /**
     * Defines additional command arguments required for this sender.
     *
     * @return List of command arguments, empty by default
     */
    fun createArguments(): List<NamedCommandArgument<out Any>> = emptyList()
}
