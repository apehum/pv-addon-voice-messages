package dev.apehum.voicemessages.playback

import dev.apehum.voicemessages.util.extension.padStartZero
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.chat.style.McTextClickEvent

fun VoiceMessage.component(clickEvent: McTextClickEvent = McTextClickEvent.runCommand("/vm-actions play $id")): McTextComponent =
    McTextComponent
        .empty()
        .append(
            McTextComponent
                .translatable("pv.addon.voice_messages.component.voice_message", duration.inWholeSeconds.padStartZero())
                .clickEvent(clickEvent),
        )

fun VoiceMessage.waveformComponents(): List<McTextComponent> {
    val components = "▁▂▃▅▆▇▉".toList()
    val componentRegion = 1.0 / components.size

    return waveform
        .map {
            components[(it / componentRegion).toInt().coerceIn(components.indices)]
        }.map { McTextComponent.literal(it.toString()) }
}
