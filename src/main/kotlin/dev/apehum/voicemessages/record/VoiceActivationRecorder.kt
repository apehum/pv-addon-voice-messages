package dev.apehum.voicemessages.record

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ListMultimap
import com.google.common.collect.Multimaps
import dev.apehum.voicemessages.util.extension.startCancellable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import su.plo.slib.api.entity.player.McPlayer
import su.plo.slib.api.event.player.McPlayerQuitEvent
import su.plo.voice.api.event.EventPriority
import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.api.server.audio.capture.ServerActivation
import su.plo.voice.api.server.audio.provider.AudioFrameProvider
import su.plo.voice.api.server.audio.provider.AudioFrameResult
import su.plo.voice.api.server.audio.source.AudioSender
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class VoiceActivationRecorder(
    val activation: ServerActivation,
    private val voiceServer: PlasmoVoiceServer,
) {
    private val contexts: ListMultimap<McPlayer, RecordContext> = Multimaps.synchronizedListMultimap(ArrayListMultimap.create())

    private val emptyFrame by lazy {
        voiceServer.createOpusEncoder(false).use { encoder ->
            val sampleRate =
                voiceServer.config?.voice()?.sampleRate()
                    ?: throw IllegalStateException("Voice config is not initialized")
            val emptyFrame = ShortArray((sampleRate / 1000) * 20)

            encoder.encode(emptyFrame)
        }
    }

    fun register(addon: Any) {
        voiceServer.eventBus.register(addon, this)
        McPlayerQuitEvent.registerListener(this::onPlayerQuit)
    }

    fun unregister(addon: Any) {
        contexts.values().forEach { context ->
            context.senderJob.cancel(CancellationException(null, Exception("Plugin shutting down")))
        }

        voiceServer.eventBus.unregister(addon, this)
        McPlayerQuitEvent.unregisterListener(this::onPlayerQuit)
    }

    /**
     * Intercepts activation for specified [duration].
     * Recording starts only after receiving first voice packet.
     *
     * @return List of encoded packets received from the player.
     */
    suspend fun record(
        player: McPlayer,
        timeout: Duration = 10.seconds,
        duration: Duration = 30.seconds,
        frameCallback: ((ByteArray, Duration) -> Unit)? = null,
    ): List<ByteArray> =
        coroutineScope {
            val frameProvider = RecordFrameProvider()
            val frames = mutableListOf<ByteArray>()

            val audioSender =
                AudioSender(
                    frameProvider,
                    { frame, _ ->
                        frames.add(frame)

                        frameCallback?.invoke(frame, (frames.size * 20L).milliseconds)

                        if (frames.size * 20L > duration.inWholeMilliseconds) {
                            frameProvider.end()
                        }

                        true
                    },
                    { true },
                )

            val senderJob =
                async {
                    withTimeout(timeout) {
                        while (true) {
                            if (!frameProvider.isEmpty()) break
                            delay(5L)
                        }
                    }

                    kotlin.runCatching {
                        audioSender.startCancellable()
                    }
                }

            val context = RecordContext(player, frameProvider, senderJob)
            contexts.put(player, context)

            try {
                senderJob.await()
            } catch (e: TimeoutCancellationException) {
                throw e
            } catch (e: RecordingStopCause) {
                throw e
            } catch (_: CancellationException) {
                // we don't want to propagate CancellationException further
                // any CancellationException that is not RecordingStopCause
                // are treated as "recording stop signal"
            }

            frames
        }

    fun stop(
        player: McPlayer,
        cause: RecordingStopCause? = null,
    ): Boolean {
        val playerContexts = contexts.removeAll(player)

        playerContexts.forEach { context ->
            context.senderJob.cancel(cause)
        }

        return playerContexts.isNotEmpty()
    }

    private fun onPlayerQuit(player: McPlayer) {
        stop(player, PlayerLeftStopCause())
    }

    @EventSubscribe(priority = EventPriority.LOWEST)
    fun onPlayerSpeak(event: PlayerSpeakEvent) {
        val packet = event.packet
        if (packet.activationId != activation.id) return

        val player = event.player.instance
        if (!contexts.containsKey(player)) return

        CoroutineScope(Dispatchers.Default).launch {
            val decryptedFrame = voiceServer.defaultEncryption.decrypt(packet.data)

            val contexts = contexts[player] ?: return@launch
            contexts.forEach { context ->
                context.frameProvider.add(decryptedFrame)
            }
        }

        event.result = ServerActivation.Result.HANDLED
    }

    inner class RecordFrameProvider : AudioFrameProvider {
        private val queue = ArrayDeque<ByteArray>()
        private var firstProvided: Boolean = false
        private var end: Boolean = false

        fun add(frame: ByteArray) {
            firstProvided = true
            queue.add(frame)
        }

        fun end() {
            end = true
        }

        fun isEmpty() = queue.isEmpty()

        override fun provide20ms(): AudioFrameResult {
            if (end) return AudioFrameResult.Finished

            val frame =
                queue.removeFirstOrNull()
                    ?: if (firstProvided) emptyFrame else null

            return AudioFrameResult.Provided(frame)
        }
    }

    private data class RecordContext(
        val player: McPlayer,
        val frameProvider: RecordFrameProvider,
        val senderJob: Job,
    )
}
