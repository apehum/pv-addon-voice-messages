package dev.apehum.voicemessages.client.gui.chat

import dev.apehum.voicemessages.client.VoiceMessagesClient
import dev.apehum.voicemessages.client.gui.render.DANGER_COLOR
import dev.apehum.voicemessages.client.gui.render.tint
import dev.apehum.voicemessages.client.gui.widget.CompositeWidget
import dev.apehum.voicemessages.client.gui.widget.TextButton
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.network.chat.Component

private const val CANCEL_KEY = "pv.addon.voice_messages.chat.cancel"
private const val CANCEL_TOOLTIP_KEY = "pv.addon.voice_messages.chat.cancel.tooltip"

private const val WIDGETS_GAP = 6
private const val CANCEL_GAP = WIDGETS_GAP * 2

private val CANCEL_COLOR = DANGER_COLOR
private val CANCEL_HOVER_COLOR = CANCEL_COLOR.tint(0.5f)

class RecordingBar(
    x: Int,
    y: Int,
    height: Int,
) : CompositeWidget(x, y, 0, height, Component.empty()) {
    private val stop = StopRecordingButton(x, y, height)
    private val elapsed = ElapsedTimeWidget(x, y, height)
    private val cancel =
        TextButton(x, y, height, CANCEL_KEY, CANCEL_COLOR, CANCEL_HOVER_COLOR) {
            VoiceMessagesClient.session?.discard()
        }

    override val children = listOf(stop, elapsed, cancel)

    init {
        cancel.setTooltip(Tooltip.create(Component.translatable(CANCEL_TOOLTIP_KEY)))

        elapsed.x = stop.x + stop.width + WIDGETS_GAP
        cancel.x = elapsed.x + elapsed.width + CANCEL_GAP

        width = cancel.x + cancel.width - x
    }
}
