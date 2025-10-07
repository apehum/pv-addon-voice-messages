package dev.apehum.voicemessages.chat

class ChatMessageSenderRegistry {
    private val senders = mutableMapOf<String, ChatMessageSender>()

    fun register(context: String, sender: ChatMessageSender) {
        senders[context] = sender
    }

    fun getSender(context: String): ChatMessageSender? = senders[context]
}