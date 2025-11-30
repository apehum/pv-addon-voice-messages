package dev.apehum.voicemessages.api.record

import java.time.Duration
import java.util.concurrent.CancellationException

sealed class RecordingStopCause(
    message: String,
) : CancellationException(message)

class RecordingTimeoutCause(
    val timeout: Duration,
) : RecordingStopCause("Recording timed out")

class PlayerLeftStopCause : RecordingStopCause("Player left the server")

class RecordingCancelCause : RecordingStopCause("Recording cancelled")

class NewRecordingStartCause : RecordingStopCause("New recording started")
