package dev.apehum.voicemessages.client.gui.menu

import dev.apehum.voicemessages.client.gui.widget.AbstractWidget
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import su.plo.lib.mod.client.render.gui.GuiCursor
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import java.awt.Color

private const val ICON_SIZE = 8

class MenuIconButton(
    message: Component,
    private val icon: ResourceLocation,
    private val color: Color,
    private val onPress: () -> Unit,
) : AbstractWidget(0, 0, ITEM_HEIGHT, ITEM_HEIGHT, message) {
    override fun renderWidget(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        val context = GuiRenderContext(guiGraphics)
        if (isHovered) context.requestCursor(GuiCursor.POINTING_HAND)

        context.fillRow(x, y, right, isHovered)

        context.blitIcon(
            icon,
            x + 1 + (width - 2 - ICON_SIZE) / 2,
            y + (ITEM_HEIGHT - 1 - ICON_SIZE) / 2,
            ICON_SIZE,
            ICON_SIZE,
            color,
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
