package dev.apehum.voicemessages.store.draft

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class MemoryVoiceMessageDraftStore(
    expireAfter: Duration = 10.minutes,
) : VoiceMessageDraftStore {
    private val cache: Cache<UUID, VoiceMessageDraft> =
        CacheBuilder
            .newBuilder()
            .expireAfterAccess(expireAfter.toJavaDuration())
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
