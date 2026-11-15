package dev.apehum.voicemessages.client.record

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import su.plo.voice.api.client.PlasmoVoiceClient

private const val FRAME_NANOS = FRAME_DURATION_MS * 1_000_000

class VoiceMessageDraftPlayer(
    private val voiceClient: PlasmoVoiceClient,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null

    @Volatile
    var playedFrames: Int = 0
        private set

    val isPlaying: Boolean
        get() = job?.isActive == true

    fun play(
        draft: VoiceMessageDraft,
        fromFrame: Int = playedFrames,
    ) {
        cancel()

        val frames = draft.trimmedFrames
        val from = fromFrame.coerceIn(0, frames.size)
        playedFrames = from

        job =
            scope.launch {
                val loopback = voiceClient.sourceManager.createLoopbackSource(true)
                loopback.initialize(false)

                try {
                    val startedAt = System.nanoTime()

                    frames.subList(from, frames.size).forEachIndexed { index, frame ->
                        ensureActive()

                        loopback.write(frame)
                        playedFrames = from + index + 1

                        val nextFrameAt = startedAt + (index + 1) * FRAME_NANOS
                        delay(((nextFrameAt - System.nanoTime()) / 1_000_000).coerceAtLeast(0L))
                    }

                    playedFrames = 0
                } finally {
                    loopback.close()
                }
            }
    }

    fun progress(draft: VoiceMessageDraft): Double {
        val frames = draft.trimEnd - draft.trimStart
        if (frames <= 0) return 0.0

        return (playedFrames.toDouble() / frames).coerceIn(0.0, 1.0)
    }

    fun toggle(draft: VoiceMessageDraft) {
        if (isPlaying) pause() else play(draft)
    }

    fun pause() {
        cancel()
    }

    fun stop() {
        cancel()
        playedFrames = 0
    }

    private fun cancel() {
        job?.cancel()
        job = null
    }
}
