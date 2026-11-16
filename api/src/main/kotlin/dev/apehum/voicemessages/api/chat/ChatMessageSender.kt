package dev.apehum.voicemessages.api.chat

import dev.apehum.voicemessages.api.VoiceMessage
import dev.apehum.voicemessages.api.command.dsl.CommandContext
import dev.apehum.voicemessages.api.command.dsl.argument.NamedCommandArgument
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.command.McCommandSource
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
    fun sendVoiceMessage(
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
    fun createContext(context: CommandContext): CompletableFuture<T>

    /**
     * Defines additional command arguments required for this sender.
     *
     * @return List of command arguments, empty by default
     */
    fun createArguments(): List<NamedCommandArgument<out Any>> = emptyList()

    /**
     * Name of this sender shown in the client mod's sender menu.
     *
     * Use [source] to resolve the name server-side, e.g. to translate it into the player's language.
     * Translatable components are resolved on the client, so their keys must exist in the client's language files.
     *
     * @param source The command source the name is shown to
     * @return The display name, or null to let the client derive one from the registered context name
     */
    fun displayName(source: McCommandSource): McTextComponent? = null
}
