package dev.apehum.voicemessages.client.gui.widget

import net.minecraft.network.chat.Component
import net.minecraft.client.gui.components.AbstractWidget as MinecraftAbstractWidget

//? if >=1.21.11 {
/*import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
*///?}

abstract class AbstractWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    message: Component,
) : MinecraftAbstractWidget(x, y, width, height, message) {
    //? if >=1.21.11 {
    /*final override fun onClick(
        event: MouseButtonEvent,
        doubled: Boolean,
    ) = handleClick(event.x, event.y)

    final override fun onRelease(event: MouseButtonEvent) = handleRelease(event.x, event.y)

    final override fun onDrag(
        event: MouseButtonEvent,
        dragX: Double,
        dragY: Double,
    ) = handleDrag(event.x, event.y, dragX, dragY)

    final override fun mouseClicked(
        event: MouseButtonEvent,
        doubled: Boolean,
    ): Boolean = handleMouseClicked(MouseInput(event, doubled))

    final override fun mouseReleased(event: MouseButtonEvent): Boolean = handleMouseReleased(MouseInput(event))

    final override fun mouseDragged(
        event: MouseButtonEvent,
        dragX: Double,
        dragY: Double,
    ): Boolean = handleMouseDragged(MouseInput(event), dragX, dragY)

    final override fun keyPressed(event: KeyEvent): Boolean = handleKeyPressed(KeyInput(event))

    final override fun charTyped(event: CharacterEvent): Boolean = handleCharTyped(CharInput(event))
    *///?} else {
    final override fun onClick(
        mouseX: Double,
        mouseY: Double,
    ) = handleClick(mouseX, mouseY)

    final override fun onRelease(
        mouseX: Double,
        mouseY: Double,
    ) = handleRelease(mouseX, mouseY)

    final override fun onDrag(
        mouseX: Double,
        mouseY: Double,
        dragX: Double,
        dragY: Double,
    ) = handleDrag(mouseX, mouseY, dragX, dragY)

    final override fun mouseClicked(
        mouseX: Double,
        mouseY: Double,
        button: Int,
    ): Boolean = handleMouseClicked(MouseInput(mouseX, mouseY, button))

    final override fun mouseReleased(
        mouseX: Double,
        mouseY: Double,
        button: Int,
    ): Boolean = handleMouseReleased(MouseInput(mouseX, mouseY, button))

    final override fun mouseDragged(
        mouseX: Double,
        mouseY: Double,
        button: Int,
        dragX: Double,
        dragY: Double,
    ): Boolean = handleMouseDragged(MouseInput(mouseX, mouseY, button), dragX, dragY)

    final override fun keyPressed(
        keyCode: Int,
        scanCode: Int,
        modifiers: Int,
    ): Boolean = handleKeyPressed(KeyInput(keyCode, scanCode, modifiers))

    final override fun charTyped(
        codePoint: Char,
        modifiers: Int,
    ): Boolean = handleCharTyped(CharInput(codePoint, modifiers))
    //?}

    protected open fun handleClick(
        mouseX: Double,
        mouseY: Double,
    ) = Unit

    protected open fun handleRelease(
        mouseX: Double,
        mouseY: Double,
    ) = Unit

    protected open fun handleDrag(
        mouseX: Double,
        mouseY: Double,
        dragX: Double,
        dragY: Double,
    ) = Unit

    protected open fun handleMouseClicked(input: MouseInput): Boolean =
        //? if >=1.21.11 {
        /*super.mouseClicked(input.event, input.doubled)
        *///?} else {
        super.mouseClicked(input.x, input.y, input.button)
        //?}

    protected open fun handleMouseReleased(input: MouseInput): Boolean =
        //? if >=1.21.11 {
        /*super.mouseReleased(input.event)
        *///?} else {
        super.mouseReleased(input.x, input.y, input.button)
        //?}

    protected open fun handleMouseDragged(
        input: MouseInput,
        dragX: Double,
        dragY: Double,
    ): Boolean =
        //? if >=1.21.11 {
        /*super.mouseDragged(input.event, dragX, dragY)
        *///?} else {
        super.mouseDragged(input.x, input.y, input.button, dragX, dragY)
        //?}

    protected open fun handleKeyPressed(input: KeyInput): Boolean =
        //? if >=1.21.11 {
        /*super.keyPressed(input.event)
        *///?} else {
        super.keyPressed(input.key, input.scanCode, input.modifiers)
        //?}

    protected open fun handleCharTyped(input: CharInput): Boolean =
        //? if >=1.21.11 {
        /*super.charTyped(input.event)
        *///?} else {
        super.charTyped(input.codePoint, input.modifiers)
        //?}
}
