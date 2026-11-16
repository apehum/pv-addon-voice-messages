package dev.apehum.voicemessages.api.storage.message

import dev.apehum.voicemessages.api.VoiceMessage
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * Storage interface for persisting voice messages.
 *
 * Implementations can use different backends (e.g., in-memory, Redis)
 * to store and retrieve voice messages by their unique ID.
 */
interface VoiceMessageStorage {
    /**
     * Retrieves a voice message by its unique ID.
     *
     * @param id The UUID of the voice message
     * @return The voice message if found, null otherwise
     */
    fun getById(id: UUID): CompletableFuture<VoiceMessage?>

    /**
     * Saves a voice message to storage.
     *
     * @param message The voice message to persist
     */
    fun save(message: VoiceMessage): CompletableFuture<Void>

    /**
     * Removes a voice message from storage.
     *
     * @param message The voice message to remove
     */
    fun remove(message: VoiceMessage): CompletableFuture<Void> = remove(message.id)

    /**
     * Removes a voice message from storage by its ID.
     *
     * @param id The UUID of the voice message to remove
     */
    fun remove(id: UUID): CompletableFuture<Void>

    /**
     * Closes the storage connection and releases resources.
     */
    fun close()
}
