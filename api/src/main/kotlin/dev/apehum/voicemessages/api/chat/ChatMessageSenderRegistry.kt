package dev.apehum.voicemessages.api.chat

/**
 * Registry for managing [ChatMessageSender] implementations by context name.
 *
 * Different chat contexts (e.g., "default" for broadcast, "direct" for direct messages)
 * can have their own sender implementations that handle voice message delivery differently.
 */
class ChatMessageSenderRegistry {
    private val senders = mutableMapOf<String, ChatMessageSender<*>>()

    /**
     * Registers a [ChatMessageSender] implementation for a specific context.
     *
     * @param context The context name (e.g., "default", "direct")
     * @param sender The sender implementation to register
     */
    fun register(
        context: String,
        sender: ChatMessageSender<*>,
    ) {
        senders[context] = sender
    }

    /**
     * Unregisters a sender by its context name.
     *
     * @param context The context name to unregister
     * @return true if a sender was removed, false if no sender was registered with that context
     */
    fun unregister(context: String): Boolean = senders.remove(context) != null

    /**
     * Returns all registered senders as a set of entries.
     *
     * @return Set of context name to sender mappings
     */
    fun getSenders(): Set<Map.Entry<String, ChatMessageSender<*>>> = senders.entries

    /**
     * Retrieves a sender by its context name.
     *
     * @param context The context name to look up
     * @return The sender if found, null otherwise
     */
    fun getSender(context: String): ChatMessageSender<*>? = senders[context]
}
