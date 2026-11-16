package dev.apehum.voicemessages.client.gui.menu

import dev.apehum.voicemessages.client.gui.widget.AbstractWidget
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import su.plo.lib.mod.client.render.Colors
import su.plo.lib.mod.client.render.gui.GuiCursor
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import java.awt.Color

class MenuRow(
    message: Component,
    private val color: Color = Colors.TEXT,
    private val marker: String? = null,
    private val centered: Boolean = false,
    private val selected: () -> Boolean = { false },
    private val onPress: () -> Unit,
) : AbstractWidget(0, 0, 0, ITEM_HEIGHT, message) {
    private val font = Minecraft.getInstance().font

    override fun renderWidget(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        val context = GuiRenderContext(guiGraphics)
        if (isHovered) context.requestCursor(GuiCursor.POINTING_HAND)

        context.fillRow(x, y, right, isHovered || selected())

        val label = message.visualOrderText
        val textTop = textY(y)
        if (centered) {
            context.drawCenteredString(label, x + width / 2, textTop, color)
        } else {
            context.drawString(label, x + TEXT_PADDING, textTop, color)
        }

        marker?.let { context.drawString(it, right - TEXT_PADDING - font.width(it), textTop, Colors.GRAY) }
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
