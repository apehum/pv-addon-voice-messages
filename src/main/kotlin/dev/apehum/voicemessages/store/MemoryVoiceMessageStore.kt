package dev.apehum.voicemessages.store

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import dev.apehum.voicemessages.playback.VoiceMessage
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class MemoryVoiceMessageStore(
    expireAfter: Duration = 10.minutes,
) : VoiceMessageStore {
    private val cache: Cache<UUID, VoiceMessage> =
        CacheBuilder
            .newBuilder()
            .expireAfterAccess(expireAfter.toJavaDuration())
            .build()

    override suspend fun getById(id: UUID): VoiceMessage? = cache.getIfPresent(id)

    override suspend fun save(message: VoiceMessage) {
        cache.put(message.id, message)
    }

    override suspend fun remove(id: UUID) {
        cache.invalidate(id)
    }
}
