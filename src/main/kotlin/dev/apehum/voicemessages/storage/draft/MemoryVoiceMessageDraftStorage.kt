package dev.apehum.voicemessages.storage.draft

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import dev.apehum.voicemessages.api.storage.draft.VoiceMessageDraft
import dev.apehum.voicemessages.api.storage.draft.VoiceMessageDraftStorage
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class MemoryVoiceMessageDraftStorage(
    expireAfter: Duration,
) : VoiceMessageDraftStorage {
    private val cache: Cache<UUID, VoiceMessageDraft> =
        CacheBuilder
            .newBuilder()
            .expireAfterAccess(expireAfter.toJavaDuration().toNanos(), TimeUnit.NANOSECONDS)
            .build()

    override fun getByPlayerId(playerId: UUID): CompletableFuture<VoiceMessageDraft?> =
        CompletableFuture.completedFuture(cache.getIfPresent(playerId))

    override fun save(
        playerId: UUID,
        draft: VoiceMessageDraft,
    ): CompletableFuture<Void> {
        cache.put(playerId, draft)
        return CompletableFuture.completedFuture(null)
    }

    override fun remove(playerId: UUID): CompletableFuture<Void> {
        cache.invalidate(playerId)
        return CompletableFuture.completedFuture(null)
    }
}
