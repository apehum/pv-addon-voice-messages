package dev.apehum.voicemessages.channel

import com.google.common.cache.CacheBuilder
import dev.apehum.voicemessages.channel.codec.SendFailure
import dev.apehum.voicemessages.channel.codec.UploadRequest
import su.plo.slib.api.server.entity.player.McServerPlayer
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

sealed interface UploadResult {
    data class Completed(
        val frames: List<ByteArray>,
    ) : UploadResult

    data class Failed(
        val failure: SendFailure,
    ) : UploadResult
}

class VoiceMessageUploads(
    expireAfter: Duration,
) {
    private val uploads =
        CacheBuilder
            .newBuilder()
            .expireAfterAccess(expireAfter.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            .build<UUID, VoiceMessageUpload>()

    fun append(
        player: McServerPlayer,
        chunk: UploadRequest,
        maxFrames: Int,
        maxBytes: Int,
    ) {
        val upload = findOrCreate(player.uuid, chunk) ?: return

        upload.append(chunk, maxFrames, maxBytes)
        uploads.put(player.uuid, upload)
    }

    fun complete(
        player: McServerPlayer,
        chunk: UploadRequest,
        maxFrames: Int,
        maxBytes: Int,
    ): UploadResult {
        val upload =
            findOrCreate(player.uuid, chunk)
                ?: return UploadResult.Failed(SendFailure.UPLOAD_EXPIRED)

        uploads.invalidate(player.uuid)
        upload.append(chunk, maxFrames, maxBytes)

        return upload.result()
    }

    private fun findOrCreate(
        playerId: UUID,
        chunk: UploadRequest,
    ): VoiceMessageUpload? {
        val current = uploads.getIfPresent(playerId)

        return when {
            current?.uploadId == chunk.uploadId -> current
            chunk.index == 0 -> VoiceMessageUpload(chunk.uploadId)
            else -> null
        }
    }
}

private class VoiceMessageUpload(
    val uploadId: String,
) {
    private val frames = mutableListOf<ByteArray>()
    private var bytes: Int = 0
    private var nextIndex: Int = 0
    private var failure: SendFailure? = null

    fun append(
        chunk: UploadRequest,
        maxFrames: Int,
        maxBytes: Int,
    ) {
        if (failure != null) return

        if (chunk.index != nextIndex) {
            failure = SendFailure.BAD_REQUEST
            return
        }
        nextIndex++

        val chunkBytes = chunk.frames.sumOf { it.size }
        if (frames.size + chunk.frames.size > maxFrames || bytes + chunkBytes > maxBytes) {
            failure = SendFailure.TOO_LONG
            return
        }

        frames += chunk.frames
        bytes += chunkBytes
    }

    fun result(): UploadResult {
        failure?.let { return UploadResult.Failed(it) }
        if (frames.isEmpty()) return UploadResult.Failed(SendFailure.BAD_REQUEST)

        return UploadResult.Completed(frames)
    }
}
