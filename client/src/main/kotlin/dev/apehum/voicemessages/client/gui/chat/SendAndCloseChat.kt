package dev.apehum.voicemessages.client.gui.chat

import dev.apehum.voicemessages.client.session.SendTarget
import dev.apehum.voicemessages.client.session.VoiceMessageSession
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.InBedChatScreen
import net.minecraft.client.gui.screens.Screen

fun VoiceMessageSession.sendAndCloseChat(target: SendTarget) {
    val chat = (currentScreen() as? ChatScreen)?.takeIf { it !is InBedChatScreen }

    send(target) {
        if (chat != null && currentScreen() === chat) chat.onClose()
    }
}

private fun currentScreen(): Screen? =
    //? if >=26.3 {
    /*Minecraft.getInstance().gui.screen()
    *///?} else {
    Minecraft.getInstance().screen
//?}
