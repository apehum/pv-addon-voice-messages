package dev.apehum.voicemessages.client.gui.widget

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import su.plo.lib.mod.client.render.RenderUtil
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import su.plo.slib.api.chat.component.McTextComponent
import java.awt.Color

//? if >=1.21.11
/*import net.minecraft.client.input.MouseButtonEvent*/

private const val FONT_HEIGHT = 8

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
        GuiRenderContext(guiGraphics).drawString(
            label,
            x,
            y + (height - FONT_HEIGHT) / 2,
            if (isHovered) hoverColor else color,
        )
    }

    //? if >=1.21.11 {
    /*override fun onClick(
        event: MouseButtonEvent,
        doubled: Boolean,
    ) {
        onPress()
    }
    *///?} else {
    override fun onClick(
        mouseX: Double,
        mouseY: Double,
    ) {
        onPress()
    }
    //?}

    override fun updateWidgetNarration(output: NarrationElementOutput) {
        output.add(NarratedElementType.TITLE, message)
    }
}
