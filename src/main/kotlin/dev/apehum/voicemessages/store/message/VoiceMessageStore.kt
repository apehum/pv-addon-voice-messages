package dev.apehum.voicemessages.store.message

import dev.apehum.voicemessages.playback.VoiceMessage
import java.util.UUID

interface VoiceMessageStore {
    suspend fun getById(id: UUID): VoiceMessage?

    suspend fun save(message: VoiceMessage)

    suspend fun remove(message: VoiceMessage) {
        remove(message.id)
    }

    suspend fun remove(id: UUID)

    fun close()
}
