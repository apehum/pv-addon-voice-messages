//? if neoforge {
/*package dev.apehum.voicemessages.client.entrypoint

import dev.apehum.voicemessages.VoiceMessagesAddon
import dev.apehum.voicemessages.client.ClientVoiceMessagesAddon
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import su.plo.voice.api.client.PlasmoVoiceClient
import su.plo.voice.api.server.PlasmoVoiceServer

@Mod("pv_addon_voice_messages")
class NeoForgeVoiceMessagesLoader(modBus: IEventBus, dist: Dist) {
    init {
        PlasmoVoiceServer.getAddonsLoader().load(VoiceMessagesAddon())

        if (dist.isClient) {
            modBus.addListener(FMLClientSetupEvent::class.java) {
                PlasmoVoiceClient.getAddonsLoader().load(ClientVoiceMessagesAddon())
            }
        }
    }
}
*///?}
