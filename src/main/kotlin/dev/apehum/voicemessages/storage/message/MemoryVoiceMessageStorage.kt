package dev.apehum.voicemessages.storage.message

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import dev.apehum.voicemessages.api.VoiceMessage
import dev.apehum.voicemessages.api.storage.message.VoiceMessageStorage
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class MemoryVoiceMessageStorage(
    expireAfter: Duration,
) : VoiceMessageStorage {
    private val cache: Cache<UUID, VoiceMessage> =
        CacheBuilder
            .newBuilder()
            .expireAfterAccess(expireAfter.toJavaDuration().toNanos(), TimeUnit.NANOSECONDS)
            .build()

    override suspend fun getById(id: UUID): VoiceMessage? = cache.getIfPresent(id)

    override suspend fun save(message: VoiceMessage) {
        cache.put(message.id, message)
    }

    override suspend fun remove(id: UUID) {
        cache.invalidate(id)
    }

    override fun close() {
        cache.invalidateAll()
    }
}
