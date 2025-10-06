package dev.apehum.voicemessages.util.extension

import kotlinx.coroutines.suspendCancellableCoroutine
import su.plo.voice.api.server.audio.source.AudioSender
import kotlin.coroutines.resume

suspend fun AudioSender.startCancellable() =
    suspendCancellableCoroutine { continuation ->
        onStop {
            continuation.resume(Unit)
        }

        continuation.invokeOnCancellation {
            stop()
        }

        start()
    }
