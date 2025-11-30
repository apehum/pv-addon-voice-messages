package dev.apehum.voicemessages.api.storage.message

import dev.apehum.voicemessages.api.VoiceMessage
import java.util.UUID

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
    suspend fun getById(id: UUID): VoiceMessage?

    /**
     * Saves a voice message to storage.
     *
     * @param message The voice message to persist
     */
    suspend fun save(message: VoiceMessage)

    /**
     * Removes a voice message from storage.
     *
     * @param message The voice message to remove
     */
    suspend fun remove(message: VoiceMessage) {
        remove(message.id)
    }

    /**
     * Removes a voice message from storage by its ID.
     *
     * @param id The UUID of the voice message to remove
     */
    suspend fun remove(id: UUID)

    /**
     * Closes the storage connection and releases resources.
     */
    fun close()
}
