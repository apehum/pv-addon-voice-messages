package dev.apehum.voicemessages.playback

import dev.apehum.voicemessages.api.VoiceMessage
import dev.apehum.voicemessages.util.extension.merge
import dev.apehum.voicemessages.util.extension.padStartZero
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.chat.style.McTextClickEvent
import java.time.Duration

fun VoiceMessage.component(clickEvent: McTextClickEvent = McTextClickEvent.runCommand("/vm-actions play $id")): McTextComponent =
    McTextComponent
        .empty()
        .append(
            McTextComponent
                .translatable(
                    "pv.addon.voice_messages.component.voice_message",
                    duration.minutesAndSeconds(),
                    waveformComponents().merge(),
                ).clickEvent(clickEvent),
        )

fun VoiceMessage.waveformComponents(): List<McTextComponent> {
    val components = "▁▂▃▅▆▇▉".toList()
    val componentRegion = 1.0 / components.size

    return waveform
        .map {
            components[(it / componentRegion).toInt().coerceIn(components.indices)]
        }.map { McTextComponent.literal(it.toString()) }
}

fun Duration.minutesAndSeconds(): McTextComponent =
    McTextComponent.literal(
        "${toMinutes().padStartZero()}:${(seconds % 60).padStartZero()}",
    )

fun kotlin.time.Duration.minutesAndSeconds(): McTextComponent =
    McTextComponent.literal(
        "${inWholeMinutes.padStartZero()}:${(inWholeSeconds % 60).padStartZero()}",
    )
