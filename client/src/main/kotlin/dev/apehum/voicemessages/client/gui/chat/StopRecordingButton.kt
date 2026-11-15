package dev.apehum.voicemessages.client.gui.chat

import dev.apehum.voicemessages.client.VoiceMessagesClient
import dev.apehum.voicemessages.client.gui.render.ACCENT_COLOR
import dev.apehum.voicemessages.client.gui.render.WaveformShape
import dev.apehum.voicemessages.client.gui.render.renderCircleWaveform
import dev.apehum.voicemessages.client.modResource
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import kotlin.math.exp
import kotlin.math.roundToInt

//? if >=1.21.11
/*import net.minecraft.client.input.MouseButtonEvent*/

private val STOP_TEXTURE = modResource("textures/gui/sprites/stop.png")
private const val STOP_TEXTURE_SIZE = 8
private const val STOP_GLYPH_SIZE = 8

private const val STOP_KEY = "pv.addon.voice_messages.chat.stop"

// todo: i am still not sure about this color; right now it's just pv's accent color
private val BUTTON_COLOR = ACCENT_COLOR

private const val BLOB_ALPHA = 0.75f
private const val BLOB_SCALE = 3.5f

private const val NOISE_FLOOR = 0.5f
private const val LEVEL_CEILING = 0.9f

private const val ATTACK_SECONDS = 0.06f
private const val RELEASE_SECONDS = 0.3f

class StopRecordingButton(
    x: Int,
    y: Int,
    size: Int,
) : AbstractWidget(x, y, size, size, Component.translatable(STOP_KEY)) {
    private val shape = WaveformShape()

    private var amplitude = 0f
    private var lastFrameNanos = 0L

    init {
        setTooltip(Tooltip.create(Component.translatable(STOP_KEY)))
    }

    override fun renderWidget(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        val context = GuiRenderContext(guiGraphics)

        val outerSize = (width * BLOB_SCALE).roundToInt() / 2 * 2

        advance()

        context.renderCircleWaveform(
            centerX = x + width / 2,
            centerY = y + height / 2,
            circleSize = width,
            outerSize = outerSize,
            amplitude = amplitude,
            shape = shape,
            color = BUTTON_COLOR,
            blobAlpha = BLOB_ALPHA,
        )

        context.blit(
            STOP_TEXTURE,
            x + (width - STOP_GLYPH_SIZE) / 2,
            y + (height - STOP_GLYPH_SIZE) / 2,
            STOP_GLYPH_SIZE,
            STOP_GLYPH_SIZE,
            0f,
            0f,
            STOP_TEXTURE_SIZE,
            STOP_TEXTURE_SIZE,
            STOP_TEXTURE_SIZE,
            STOP_TEXTURE_SIZE,
        )
    }

    //? if >=1.21.11 {
    /*override fun onClick(
        event: MouseButtonEvent,
        doubled: Boolean,
    ) {
        VoiceMessagesClient.session?.recorder?.stop()
    }
    *///?} else {
    override fun onClick(
        mouseX: Double,
        mouseY: Double,
    ) {
        VoiceMessagesClient.session?.recorder?.stop()
    }
    //?}

    override fun updateWidgetNarration(output: NarrationElementOutput) {
        output.add(NarratedElementType.TITLE, message)
    }

    private fun advance() {
        val now = System.nanoTime()
        val delta = if (lastFrameNanos == 0L) 0f else (now - lastFrameNanos) / 1_000_000_000f
        lastFrameNanos = now

        val level = VoiceMessagesClient.session?.recorder?.recentLevels(1)?.lastOrNull()?.toFloat() ?: 0f
        val target = ((level - NOISE_FLOOR) / (LEVEL_CEILING - NOISE_FLOOR)).coerceIn(0f, 1f)

        val smoothingSeconds = if (target > amplitude) ATTACK_SECONDS else RELEASE_SECONDS
        amplitude += (target - amplitude) * (1f - exp(-delta / smoothingSeconds))

        shape.advance(amplitude, delta)
    }
}
