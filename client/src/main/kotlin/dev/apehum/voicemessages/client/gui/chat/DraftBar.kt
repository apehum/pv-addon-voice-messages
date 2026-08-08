package dev.apehum.voicemessages.client.gui.chat

import dev.apehum.voicemessages.client.VoiceMessagesClient
import dev.apehum.voicemessages.client.gui.render.ACCENT_COLOR
import dev.apehum.voicemessages.client.gui.render.DANGER_COLOR
import dev.apehum.voicemessages.client.gui.render.tint
import dev.apehum.voicemessages.client.gui.widget.CompositeWidget
import dev.apehum.voicemessages.client.gui.widget.IconButton
import dev.apehum.voicemessages.client.modResource
import net.minecraft.network.chat.Component

private val DELETE_ICON = modResource("textures/gui/sprites/delete.png")
private val SEND_ICON = modResource("textures/gui/sprites/send.png")

private const val DELETE_KEY = "pv.addon.voice_messages.chat.delete"
private const val SEND_KEY = "pv.addon.voice_messages.chat.send"

private const val ELEMENTS_GAP = 2

private val DELETE_HOVER_COLOR = DANGER_COLOR
private val SEND_COLOR = ACCENT_COLOR.tint(0.3f)
private val SEND_HOVER_COLOR = ACCENT_COLOR.tint(0.6f)

class DraftBar(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
) : CompositeWidget(x, y, width, height, Component.empty()) {
    private val delete =
        IconButton(
            x,
            y,
            height,
            DELETE_KEY,
            DELETE_ICON,
            hoverColor = DELETE_HOVER_COLOR,
        ) { VoiceMessagesClient.discard() }

    private val send =
        IconButton(
            x + width - height,
            y,
            height,
            SEND_KEY,
            SEND_ICON,
            color = SEND_COLOR,
            hoverColor = SEND_HOVER_COLOR,
        ) { VoiceMessagesClient.send() }

    private val track = DraftTrack(x, y, 0, height)

    override val children = listOf(delete, track, send)

    init {
        track.x = delete.x + delete.width + ELEMENTS_GAP
        track.width = send.x - ELEMENTS_GAP - track.x
    }
}
