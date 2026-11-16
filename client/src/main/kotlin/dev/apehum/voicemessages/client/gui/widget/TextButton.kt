package dev.apehum.voicemessages.client.gui.widget

import dev.apehum.voicemessages.client.gui.render.TEXT_HEIGHT
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import su.plo.lib.mod.client.render.RenderUtil
import su.plo.lib.mod.client.render.gui.GuiCursor
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import su.plo.slib.api.chat.component.McTextComponent
import java.awt.Color

class TextButton(
    x: Int,
    y: Int,
    height: Int,
    translationKey: String,
    private val color: Color,
    private val hoverColor: Color,
    private val onPress: () -> Unit,
) : AbstractWidget(x, y, 0, height, Component.translatable(translationKey)) {
    private val label = McTextComponent.translatable(translationKey)

    init {
        width = RenderUtil.getTextWidth(label)
    }

    override fun renderWidget(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        val context = GuiRenderContext(guiGraphics)
        if (isHovered) context.requestCursor(GuiCursor.POINTING_HAND)

        context.drawString(
            label,
            x,
            y + (height - TEXT_HEIGHT) / 2,
            if (isHovered) hoverColor else color,
        )
    }

    override fun handleClick(
        mouseX: Double,
        mouseY: Double,
    ) {
        onPress()
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) {
        output.add(NarratedElementType.TITLE, message)
    }
}
