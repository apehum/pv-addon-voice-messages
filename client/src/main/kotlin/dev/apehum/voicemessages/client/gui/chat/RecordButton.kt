package dev.apehum.voicemessages.client.gui.chat

import dev.apehum.voicemessages.client.VoiceMessagesClient
import dev.apehum.voicemessages.client.gui.widget.IconButton
import dev.apehum.voicemessages.client.modResource
import su.plo.lib.mod.client.render.Colors

private val RECORD_ICON = modResource("textures/gui/sprites/record.png")
private val RECORD_ICON_COLOR = Colors.WHITE

private const val RECORD_KEY = "pv.addon.voice_messages.chat.record"

class RecordButton(
    x: Int,
    y: Int,
    size: Int,
) : IconButton(
        x,
        y,
        size,
        RECORD_KEY,
        RECORD_ICON,
        color = RECORD_ICON_COLOR,
        background = true,
        onPress = { VoiceMessagesClient.startRecording() },
    )
