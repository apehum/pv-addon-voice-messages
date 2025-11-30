package dev.apehum.voicemessages.storage.draft

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import dev.apehum.voicemessages.api.storage.draft.VoiceMessageDraft
import dev.apehum.voicemessages.api.storage.draft.VoiceMessageDraftStorage
import java.util.UUID
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

    override suspend fun getByPlayerId(playerId: UUID): VoiceMessageDraft? = cache.getIfPresent(playerId)

    override suspend fun save(
        playerId: UUID,
        draft: VoiceMessageDraft,
    ) {
        cache.put(playerId, draft)
    }

    override suspend fun remove(playerId: UUID) {
        cache.invalidate(playerId)
    }
}
