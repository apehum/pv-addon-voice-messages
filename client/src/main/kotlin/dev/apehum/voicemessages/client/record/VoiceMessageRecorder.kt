package dev.apehum.voicemessages.client.record

import su.plo.voice.api.client.PlasmoVoiceClient
import su.plo.voice.api.client.event.audio.capture.AudioCaptureEvent
import su.plo.voice.api.client.event.audio.capture.AudioCaptureProcessedEvent
import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.api.util.AudioUtil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class VoiceMessageRecorder(
    private val voiceClient: PlasmoVoiceClient,
    private val maxDuration: Duration,
) {
    private val frames = mutableListOf<ShortArray>()
    private val levels = mutableListOf<Double>()

    @Volatile
    var isRecording: Boolean = false
        private set

    @Volatile
    var draft: VoiceMessageDraft? = null
        private set

    val elapsed: Duration
        get() = synchronized(frames) { (frames.size * FRAME_DURATION_MS).milliseconds }

    fun recentLevels(count: Int): List<Double> = synchronized(frames) { levels.takeLast(count) }

    fun canRecord(): Boolean =
        voiceClient.serverInfo.isPresent &&
            !voiceClient.audioCapture.isServerMuted &&
            !voiceClient.config.voice.disabled
                .value() &&
            !voiceClient.config.voice.microphoneDisabled
                .value()

    fun start(): Boolean {
        if (isRecording || !canRecord()) return false

        synchronized(frames) {
            frames.clear()
            levels.clear()
        }
        draft = null
        isRecording = true

        return true
    }

    fun stop(): VoiceMessageDraft? {
        if (!isRecording) return null

        isRecording = false
        return finish()
    }

    fun cancel() {
        isRecording = false
        draft = null
        synchronized(frames) {
            frames.clear()
            levels.clear()
        }
    }

    @EventSubscribe
    fun onAudioCapture(event: AudioCaptureEvent) {
        if (!isRecording) return

        event.isFlushActivations = true
    }

    @EventSubscribe
    fun onAudioCaptureProcessed(event: AudioCaptureProcessedEvent) {
        if (!isRecording) return

        if (!canRecord()) {
            isRecording = false
            finish()
            return
        }

        val samples = event.processed.mono

        val reachedMaxDuration =
            synchronized(frames) {
                frames += samples
                levels += samples.audioLevel()
                frames.size >= maxFrames
            }

        if (reachedMaxDuration) {
            isRecording = false
            finish()
        }
    }

    private fun finish(): VoiceMessageDraft? {
        val recorded = synchronized(frames) { frames.toList() }
        if (recorded.isEmpty()) return null

        return VoiceMessageDraft(recorded).also { draft = it }
    }

    private val maxFrames: Int
        get() = (maxDuration.inWholeMilliseconds / FRAME_DURATION_MS).toInt()
}

private fun ShortArray.audioLevel(): Double = 1.0 - (AudioUtil.calculateAudioLevel(this, 0, size) / -127.0)
