package dev.apehum.voicemessages.storage.message

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import dev.apehum.voicemessages.api.VoiceMessage
import dev.apehum.voicemessages.api.storage.message.VoiceMessageStorage
import java.util.UUID
import java.util.concurrent.CompletableFuture
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

    override fun getById(id: UUID): CompletableFuture<VoiceMessage?> =
        CompletableFuture.completedFuture(cache.getIfPresent(id))

    override fun save(message: VoiceMessage): CompletableFuture<Void> {
        cache.put(message.id, message)
        return CompletableFuture.completedFuture(null)
    }

    override fun remove(id: UUID): CompletableFuture<Void> {
        cache.invalidate(id)
        return CompletableFuture.completedFuture(null)
    }

    override fun close() {
        cache.invalidateAll()
    }
}
