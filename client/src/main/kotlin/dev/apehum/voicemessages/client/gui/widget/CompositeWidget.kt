package dev.apehum.voicemessages.client.gui.widget

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import net.minecraft.client.gui.components.AbstractWidget as MinecraftAbstractWidget

abstract class CompositeWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    message: Component,
) : AbstractWidget(x, y, width, height, message) {
    protected abstract val children: List<MinecraftAbstractWidget>

    private var pressed: MinecraftAbstractWidget? = null

    override fun renderWidget(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        //~ if >=26.3 'render' -> 'extractRenderState'
        children.forEach { it.render(guiGraphics, mouseX, mouseY, partialTick) }
    }

    override fun handleMouseClicked(input: MouseInput): Boolean {
        if (!active || !visible) return false

        pressed = children.firstOrNull { it.mouseClicked(input) }
        return pressed != null
    }

    override fun handleMouseReleased(input: MouseInput): Boolean {
        val widget = pressed ?: return false
        pressed = null

        return widget.mouseReleased(input)
    }

    override fun handleMouseDragged(
        input: MouseInput,
        dragX: Double,
        dragY: Double,
    ): Boolean = pressed?.mouseDragged(input, dragX, dragY) == true

    override fun handleKeyPressed(input: KeyInput): Boolean = children.any { it.isFocused && it.keyPressed(input) }

    override fun handleCharTyped(input: CharInput): Boolean = children.any { it.isFocused && it.charTyped(input) }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        scrollX: Double,
        scrollY: Double,
    ): Boolean = children.any { it.isMouseOver(mouseX, mouseY) && it.mouseScrolled(mouseX, mouseY, scrollX, scrollY) }

    override fun updateWidgetNarration(output: NarrationElementOutput) {
        children.forEach { it.updateNarration(output.nest()) }
    }
}
