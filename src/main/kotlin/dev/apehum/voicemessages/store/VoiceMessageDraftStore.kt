package dev.apehum.voicemessages.store

import dev.apehum.voicemessages.playback.VoiceMessage
import java.util.UUID

data class VoiceMessageDraft(
    val message: VoiceMessage,
    val chatContext: String,
)

interface VoiceMessageDraftStore {
    suspend fun getByPlayerId(playerId: UUID): VoiceMessageDraft?

    suspend fun save(playerId: UUID, draft: VoiceMessageDraft)

    suspend fun remove(playerId: UUID)
}