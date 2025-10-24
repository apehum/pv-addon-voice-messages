package dev.apehum.voicemessages.record

import kotlin.coroutines.cancellation.CancellationException

sealed class RecordingStopCause(
    message: String,
) : CancellationException(message)

class PlayerLeftStopCause : RecordingStopCause("Player left the server")

class RecordingCancelCause : RecordingStopCause("Recording cancelled")

class NewRecordingStartCause : RecordingStopCause("New recording started")
