package dev.apehum.voicemessages.api

import dev.apehum.voicemessages.chat.ChatMessageSender
import dev.apehum.voicemessages.chat.ChatMessageSenderRegistry
import dev.apehum.voicemessages.playback.VoiceMessage
import dev.apehum.voicemessages.storage.draft.VoiceMessageDraftStorage
import dev.apehum.voicemessages.storage.message.VoiceMessageStorage
import su.plo.voice.api.server.player.VoiceServerPlayer

/**
 * API for the Voice Messages addon.
 *
 * Provides access to voice message storage, draft management, and playback functionality.
 */
interface VoiceMessagesAPI {
    /**
     * Registry for managing different chat message sender implementations.
     *
     * Use this to register custom [ChatMessageSender] implementations for different chat contexts.
     * Built-in implementations include "default" for broadcast messages and "direct" for direct messages.
     */
    val messageSenderRegistry: ChatMessageSenderRegistry

    /**
     * Storage for persisting and retrieving voice messages by their unique ID.
     */
    val messageStorage: VoiceMessageStorage

    /**
     * Storage for managing voice message drafts after recording.
     *
     * Drafts are stored by player ID and contain the recorded message along with
     * the chat context in which it will be sent.
     */
    val draftMessageStorage: VoiceMessageDraftStorage

    /**
     * Plays a voice message to a specific player with optional waveform visualization.
     *
     * The message will be played through the addon's audio source line, displaying
     * duration and waveform visualization to the player.
     *
     * @param player The player who will hear the voice message
     * @param voiceMessage The voice message to play
     * @param showWaveform Whether the waveform should be shown to the player in overlay
     */
    fun playVoiceMessage(
        player: VoiceServerPlayer,
        voiceMessage: VoiceMessage,
        showWaveform: Boolean = false,
    )

    /**
     * Stops voice message playback for a specific player.
     *
     * @param player The player whose voice message playback should be stopped
     * @return true if a voice message was playing and was stopped, false if no message was playing
     */
    fun stopVoiceMessage(player: VoiceServerPlayer): Boolean
}
