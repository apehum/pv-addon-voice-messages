package dev.apehum.voicemessages.client.gui.chat

import dev.apehum.voicemessages.client.VoiceMessagesClient
import dev.apehum.voicemessages.client.gui.menu.SenderMenu
import dev.apehum.voicemessages.client.gui.render.DANGER_COLOR
import dev.apehum.voicemessages.client.gui.widget.CompositeWidget
import dev.apehum.voicemessages.client.gui.widget.IconButton
import dev.apehum.voicemessages.client.modResource
import net.minecraft.network.chat.Component

private const val ELEMENTS_GAP = 2

class DraftBar(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    menu: SenderMenu,
) : CompositeWidget(x, y, width, height, Component.empty()) {
    private val delete =
        IconButton(
            x,
            y,
            height,
            "pv.addon.voice_messages.chat.delete",
            modResource("textures/gui/sprites/delete.png"),
            hoverColor = DANGER_COLOR,
        ) { VoiceMessagesClient.session?.discard() }

    private val send = SendButton(x + width - height, y, height, menu)

    private val track = DraftTrack(x, y, 0, height)

    override val children = listOf(delete, track, send)

    init {
        track.x = delete.x + delete.width + ELEMENTS_GAP
        track.width = send.x - ELEMENTS_GAP - track.x
    }
}
