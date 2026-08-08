//? if fabric {
package dev.apehum.voicemessages.client.entrypoint

import dev.apehum.voicemessages.VoiceMessagesAddon
import dev.apehum.voicemessages.client.ClientVoiceMessagesAddon
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import su.plo.voice.api.client.PlasmoVoiceClient
import su.plo.voice.api.server.PlasmoVoiceServer

class FabricVoiceMessagesLoader :
    ClientModInitializer,
    ModInitializer {
    override fun onInitialize() {
        PlasmoVoiceServer.getAddonsLoader().load(VoiceMessagesAddon())
    }

    override fun onInitializeClient() {
        PlasmoVoiceClient.getAddonsLoader().load(ClientVoiceMessagesAddon())
    }
}
//?}
