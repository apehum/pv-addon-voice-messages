package dev.apehum.voicemessages.api.event

import dev.apehum.voicemessages.api.chat.ChatMessageSenderRegistry
import su.plo.slib.api.event.GlobalEvent

/**
 * An event fired on message senders registration.
 */
object MessageSenderRegistrationEvent : GlobalEvent<MessageSenderRegistrationEvent.Callback>(
    { callbacks ->
        Callback { registry ->
            callbacks.forEach { callback -> callback.onRegistration(registry) }
        }
    },
) {
    fun interface Callback {
        fun onRegistration(registry: ChatMessageSenderRegistry)
    }
}
