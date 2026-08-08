package dev.apehum.voicemessages.client

import su.plo.voice.api.addon.AddonInitializer
import su.plo.voice.api.addon.AddonLoaderScope
import su.plo.voice.api.addon.InjectPlasmoVoice
import su.plo.voice.api.addon.annotation.Addon
import su.plo.voice.api.client.PlasmoVoiceClient
import su.plo.voice.api.client.event.socket.UdpClientClosedEvent
import su.plo.voice.api.event.EventSubscribe

@Addon(
    id = BuildConfig.ADDON_ID,
    version = BuildConfig.VERSION,
    authors = ["Apehum"],
    scope = AddonLoaderScope.CLIENT,
)
class ClientVoiceMessagesAddon : AddonInitializer {
    @InjectPlasmoVoice
    private lateinit var voiceClient: PlasmoVoiceClient

    override fun onAddonInitialize() {
        val recorder = VoiceMessagesClient.initialize(voiceClient)
        voiceClient.eventBus.register(this, recorder)
    }

    @EventSubscribe
    fun onVoiceDisconnect(event: UdpClientClosedEvent) {
        VoiceMessagesClient.discard()
    }
}
