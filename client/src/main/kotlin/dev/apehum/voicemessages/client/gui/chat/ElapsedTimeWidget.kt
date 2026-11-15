package dev.apehum.voicemessages.client.gui.chat

import dev.apehum.voicemessages.client.VoiceMessagesClient
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import su.plo.lib.mod.client.render.RenderUtil
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import java.awt.Color
import kotlin.time.Duration

private const val FONT_HEIGHT = 8
private const val WIDEST_ELAPSED = "00:00"

class ElapsedTimeWidget(
    x: Int,
    y: Int,
    height: Int,
) : AbstractWidget(x, y, RenderUtil.getStringWidth(WIDEST_ELAPSED), height, Component.empty()) {
    init {
        active = false
    }

    override fun renderWidget(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        GuiRenderContext(guiGraphics).drawString(
            elapsed().formatted(),
            x,
            y + (height - FONT_HEIGHT) / 2,
            Color.WHITE,
        )
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) {
        output.add(
            NarratedElementType.TITLE,
            Component.literal(elapsed().formatted()),
        )
    }
}

private fun elapsed(): Duration = VoiceMessagesClient.session?.recorder?.elapsed ?: Duration.ZERO

private fun Duration.formatted(): String = "%02d:%02d".format(inWholeMinutes, inWholeSeconds % 60)
