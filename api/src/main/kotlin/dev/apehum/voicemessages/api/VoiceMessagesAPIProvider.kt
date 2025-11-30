package dev.apehum.voicemessages.api

import org.jetbrains.annotations.ApiStatus

object VoiceMessagesAPIProvider {
    private var instance: VoiceMessagesAPI? = null

    @JvmStatic
    fun getInstance(): VoiceMessagesAPI = instance ?: throw IllegalStateException("VoiceMessagesAPI is not initialized")

    @ApiStatus.Internal
    fun setInstance(api: VoiceMessagesAPI) {
        instance = api
    }
}
