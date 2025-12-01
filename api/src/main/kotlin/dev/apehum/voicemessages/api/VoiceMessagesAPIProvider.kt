package dev.apehum.voicemessages.api

import org.jetbrains.annotations.ApiStatus

/**
 * Provides access to [VoiceMessagesAPI] instance.
 *
 * Accessing the API before it has been initialized will result in an
 * [IllegalStateException].
 */
object VoiceMessagesAPIProvider {
    private var instance: VoiceMessagesAPI? = null

    /**
     * Returns the [VoiceMessagesAPI] instance.
     *
     * @throws IllegalStateException if the API has not been initialized via [setInstance].
     */
    @JvmStatic
    fun getInstance(): VoiceMessagesAPI = instance ?: throw IllegalStateException("VoiceMessagesAPI is not initialized")

    /**
     * Sets or replaces the [VoiceMessagesAPI] instance.
     *
     * @param api The [VoiceMessagesAPI] instance
     */
    @ApiStatus.Internal
    fun setInstance(api: VoiceMessagesAPI) {
        instance = api
    }
}
