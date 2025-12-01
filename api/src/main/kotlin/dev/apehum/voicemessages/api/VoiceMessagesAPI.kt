package dev.apehum.voicemessages.api

import dev.apehum.voicemessages.api.chat.ChatMessageSender
import dev.apehum.voicemessages.api.chat.ChatMessageSenderRegistry
import dev.apehum.voicemessages.api.event.MessageSenderRegistrationEvent
import dev.apehum.voicemessages.api.record.RecordingStopCause
import dev.apehum.voicemessages.api.storage.draft.VoiceMessageDraftStorage
import dev.apehum.voicemessages.api.storage.message.VoiceMessageStorage
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.voice.api.server.player.VoiceServerPlayer
import java.util.concurrent.CompletableFuture

/**
 * API for the Voice Messages addon.
 *
 * Provides access to voice message storage, draft management, and playback functionality.
 */
interface VoiceMessagesAPI {
    /**
     * Registry for managing different chat message sender implementations.
     *
     * To register custom [ChatMessageSender], do it in [MessageSenderRegistrationEvent],
     * otherwise it'll be missing in `/vm` command.
     *
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
     * Prompts a player to record voice message.
     *
     * @param player The player who will be prompted to record voice message
     *
     * @return A new [VoiceMessage] or exception. See [RecordingStopCause].
     */
    fun recordVoiceMessage(player: VoiceServerPlayer): CompletableFuture<VoiceMessage>

    /**
     * Stops voice message playback for a specific player.
     *
     * @param player The player whose voice message playback should be stopped
     * @return true if a voice message was playing and was stopped, false if no message was playing
     */
    fun stopVoiceMessage(player: VoiceServerPlayer): Boolean

    /**
     * Creates a voice message from encoded audio frames.
     *
     * This function decodes the frames to generate a waveform visualization
     * and creates a [VoiceMessage] with a unique ID.
     *
     * @param encodedFrames List of Opus-encoded audio frames
     * @return A new [VoiceMessage] with generated waveform
     */
    fun createVoiceMessage(encodedFrames: List<ByteArray>): VoiceMessage

    /**
     * Formats voice message to [McTextComponent].
     *
     * @param voiceMessage Voice message to format
     * @return A new [McTextComponent] with formatted voice message.
     */
    fun formatVoiceMessage(voiceMessage: VoiceMessage): McTextComponent

    /**
     * Formats voice message to json text component.
     *
     * @param voiceMessage Voice message to format
     * @return A json text component of formatted voice message.
     */
    fun formatVoiceMessageToJson(voiceMessage: VoiceMessage): String

    /**
     * Formats and translates voice message to json text component.
     *
     * @param voiceMessage Voice message to format
     * @param language Language to translate translatable components into
     * @return A json text component of formatted voice message.
     */
    fun formatVoiceMessageToJson(
        voiceMessage: VoiceMessage,
        language: String,
    ): String

    /**
     * Wraps the Opus-encoded audio data from the given [VoiceMessage] into an Ogg container.
     *
     * @param voiceMessage The voice message whose Opus data should be wrapped into an Ogg container.
     * @return A byte array containing the Ogg Opus container.
     */
    fun wrapVoiceMessageInOgg(voiceMessage: VoiceMessage): ByteArray
}
