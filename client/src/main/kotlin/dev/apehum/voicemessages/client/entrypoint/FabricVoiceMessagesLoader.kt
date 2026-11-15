//? if fabric {
package dev.apehum.voicemessages.client.entrypoint

import dev.apehum.voicemessages.VoiceMessagesAddon
import dev.apehum.voicemessages.client.ClientVoiceMessagesAddon
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import su.plo.voice.api.client.PlasmoVoiceClient
import su.plo.voice.api.server.PlasmoVoiceServer

//? if <1.21.6 {
import dev.apehum.voicemessages.client.gui.render.WaveformShader
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback
//?}

class FabricVoiceMessagesLoader :
    ClientModInitializer,
    ModInitializer {
    override fun onInitialize() {
        PlasmoVoiceServer.getAddonsLoader().load(VoiceMessagesAddon())
    }

    override fun onInitializeClient() {
        PlasmoVoiceClient.getAddonsLoader().load(ClientVoiceMessagesAddon())

        //? if <1.21.6 {
        CoreShaderRegistrationCallback.EVENT.register { context ->
            WaveformShader.register {
                context.register(WaveformShader.LOCATION, WaveformShader.VERTEX_FORMAT) { WaveformShader.instance = it }
            }
        }
        //?}
    }
}
//?}
