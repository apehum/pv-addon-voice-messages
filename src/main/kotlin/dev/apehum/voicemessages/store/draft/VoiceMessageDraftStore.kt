package dev.apehum.voicemessages.store.draft

import dev.apehum.voicemessages.chat.ChatContext
import dev.apehum.voicemessages.playback.VoiceMessage
import java.util.UUID

data class VoiceMessageDraft(
    val message: VoiceMessage,
    val chatSenderName: String,
    val chatContext: ChatContext,
)

interface VoiceMessageDraftStore {
    suspend fun getByPlayerId(playerId: UUID): VoiceMessageDraft?

    suspend fun save(
        playerId: UUID,
        draft: VoiceMessageDraft,
    )

    suspend fun remove(playerId: UUID)
}
