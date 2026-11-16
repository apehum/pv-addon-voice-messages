package dev.apehum.voicemessages.client.gui.chat

import dev.apehum.voicemessages.client.VoiceMessagesClient
import dev.apehum.voicemessages.client.gui.widget.IconButton
import dev.apehum.voicemessages.client.modResource
import su.plo.lib.mod.client.render.Colors

class RecordButton(
    x: Int,
    y: Int,
    size: Int,
) : IconButton(
        x,
        y,
        size,
        "pv.addon.voice_messages.chat.record",
        modResource("textures/gui/sprites/record.png"),
        color = Colors.WHITE,
        background = true,
        onPress = { VoiceMessagesClient.session?.startRecording() },
    )
