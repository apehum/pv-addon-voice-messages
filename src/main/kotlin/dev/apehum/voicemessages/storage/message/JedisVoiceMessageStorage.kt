package dev.apehum.voicemessages.storage.message

import com.google.common.io.ByteStreams
import dev.apehum.voicemessages.AddonConfig.RedisStorageConfig
import dev.apehum.voicemessages.BuildConfig
import dev.apehum.voicemessages.playback.VoiceMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisPooled
import java.util.Base64
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

fun createJedisStore(config: RedisStorageConfig): JedisVoiceMessageStorage {
    val configBuilder = DefaultJedisClientConfig.builder()
    if (config.user.isNotBlank()) {
        configBuilder.user(config.user)
    }

    if (config.password.isNotBlank()) {
        configBuilder.password(config.password)
    }

    val jedisPool = JedisPooled(HostAndPort(config.host, config.port), configBuilder.build())

    return JedisVoiceMessageStorage(jedisPool)
}

class JedisVoiceMessageStorage(
    private val jedis: JedisPooled,
    private val expireAfter: Duration = 10.minutes,
) : VoiceMessageStorage {
    override suspend fun getById(id: UUID): VoiceMessage? =
        withContext(Dispatchers.IO) {
            val messageKey = id.asKey()

            jedis
                .get(messageKey)
                ?.let {
                    jedis.expire(messageKey, expireAfter.inWholeSeconds)
                    fromBase64(it)
                }
        }

    override suspend fun save(message: VoiceMessage) {
        withContext(Dispatchers.IO) {
            val messageKey = message.id.asKey()

            jedis.set(messageKey, message.base64())
            jedis.expire(messageKey, expireAfter.inWholeSeconds)
        }
    }

    override suspend fun remove(id: UUID) {
        withContext(Dispatchers.IO) {
            jedis.del(id.asKey())
        }
    }

    override fun close() {
        jedis.close()
    }

    private fun UUID.asKey() = "${BuildConfig.PROJECT_NAME}:$this"

    private fun fromBase64(base64: String): VoiceMessage {
        val buffer = ByteStreams.newDataInput(Base64.getDecoder().decode(base64))

        val id =
            UUID(
                buffer.readLong(),
                buffer.readLong(),
            )

        val encodedFramesSize = buffer.readInt()
        val encodedFrames =
            (0 until encodedFramesSize).map {
                val frameSize = buffer.readInt()
                val frame = ByteArray(frameSize)
                buffer.readFully(frame)

                frame
            }

        val waveformSize = buffer.readInt()
        val waveform = (0 until waveformSize).map { buffer.readDouble() }

        return VoiceMessage(id, encodedFrames, waveform)
    }

    private fun VoiceMessage.base64(): String {
        val buffer = ByteStreams.newDataOutput()

        buffer.writeLong(id.mostSignificantBits)
        buffer.writeLong(id.leastSignificantBits)

        buffer.writeInt(encodedFrames.size)
        encodedFrames.forEach { frame ->
            buffer.writeInt(frame.size)
            buffer.write(frame)
        }

        buffer.writeInt(waveform.size)
        waveform.forEach { buffer.writeDouble(it) }

        return Base64.getEncoder().encodeToString(buffer.toByteArray())
    }
}
