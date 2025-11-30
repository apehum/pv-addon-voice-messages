package dev.apehum.voicemessages.api.storage.draft

import dev.apehum.voicemessages.api.VoiceMessage
import dev.apehum.voicemessages.api.chat.ChatContext
import java.util.UUID

/**
 * Represents a voice message draft recorded by a player.
 *
 * @property message The recorded voice message
 * @property chatSenderName The name of the chat sender that will deliver the message (e.g., "default", "direct")
 * @property chatContext Additional context information required by the chat sender
 */
data class VoiceMessageDraft(
    val message: VoiceMessage,
    val chatSenderName: String,
    val chatContext: ChatContext,
)

/**
 * Storage interface for managing voice message drafts after recording.
 *
 * Drafts are stored by player ID and contain the recorded message along with
 * the chat context in which it will be sent.
 */
interface VoiceMessageDraftStorage {
    /**
     * Retrieves a draft for a specific player.
     *
     * @param playerId The UUID of the player
     * @return The player's draft if it exists, null otherwise
     */
    suspend fun getByPlayerId(playerId: UUID): VoiceMessageDraft?

    /**
     * Saves a draft for a specific player.
     *
     * @param playerId The UUID of the player
     * @param draft The draft to save
     */
    suspend fun save(
        playerId: UUID,
        draft: VoiceMessageDraft,
    )

    /**
     * Removes a draft for a specific player.
     *
     * @param playerId The UUID of the player
     */
    suspend fun remove(playerId: UUID)
}
