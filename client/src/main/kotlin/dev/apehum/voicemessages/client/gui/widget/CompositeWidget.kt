package dev.apehum.voicemessages.client.gui.widget

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component

//? if >=1.21.11
/*import net.minecraft.client.input.MouseButtonEvent*/

abstract class CompositeWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    message: Component,
) : AbstractWidget(x, y, width, height, message) {
    protected abstract val children: List<AbstractWidget>

    private var pressed: AbstractWidget? = null

    override fun renderWidget(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        //~ if >=26.3 'render' -> 'extractRenderState'
        children.forEach { it.render(guiGraphics, mouseX, mouseY, partialTick) }
    }

    //? if >=1.21.11 {
    /*override fun mouseClicked(
        event: MouseButtonEvent,
        doubled: Boolean,
    ): Boolean {
        if (!active || !visible) return false

        pressed = children.firstOrNull { it.mouseClicked(event, doubled) }
        return pressed != null
    }

    override fun mouseDragged(
        event: MouseButtonEvent,
        dragX: Double,
        dragY: Double,
    ): Boolean = pressed?.mouseDragged(event, dragX, dragY) == true

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        val widget = pressed ?: return false
        pressed = null

        return widget.mouseReleased(event)
    }
    *///?} else {
    override fun mouseClicked(
        mouseX: Double,
        mouseY: Double,
        button: Int,
    ): Boolean {
        if (!active || !visible) return false

        pressed = children.firstOrNull { it.mouseClicked(mouseX, mouseY, button) }
        return pressed != null
    }

    override fun mouseDragged(
        mouseX: Double,
        mouseY: Double,
        button: Int,
        dragX: Double,
        dragY: Double,
    ): Boolean = pressed?.mouseDragged(mouseX, mouseY, button, dragX, dragY) == true

    override fun mouseReleased(
        mouseX: Double,
        mouseY: Double,
        button: Int,
    ): Boolean {
        val widget = pressed ?: return false
        pressed = null

        return widget.mouseReleased(mouseX, mouseY, button)
    }
    //?}

    override fun updateWidgetNarration(output: NarrationElementOutput) {
        children.forEach { it.updateNarration(output.nest()) }
    }
}
