package dev.apehum.voicemessages.api.record

import java.time.Duration
import java.util.concurrent.CancellationException

/**
 * Represents a reason why a recording operation stopped.
 */
sealed class RecordingStopCause(
    message: String,
) : CancellationException(message)

/**
 * Indicates that a recording stopped because the player did not speak within the expected timeout.
 *
 * @param timeout The maximum allowed time to wait for the player's initial audio input before cancelling the recording.
 */
class RecordingTimeoutCause(
    val timeout: Duration,
) : RecordingStopCause("Recording timed out")

/**
 * Indicates that a recording was stopped because the player left the server.
 */
class PlayerLeftStopCause : RecordingStopCause("Player left the server")

/**
 * Indicates that a recording was cancelled by user.
 */
class RecordingCancelCause : RecordingStopCause("Recording cancelled")

/**
 * Indicates that a recording was stopped because a new recording was started,
 * replacing the current one.
 */
class NewRecordingStartCause : RecordingStopCause("New recording started")
