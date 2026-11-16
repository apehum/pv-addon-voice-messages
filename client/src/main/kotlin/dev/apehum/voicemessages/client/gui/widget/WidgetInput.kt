package dev.apehum.voicemessages.client.gui.widget

import net.minecraft.client.gui.components.events.GuiEventListener

//? if >=1.21.11 {
/*import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
*///?}

//? if >=1.21.11 {
/*class MouseInput(
    val event: MouseButtonEvent,
    val doubled: Boolean = false,
) {
    val x: Double
        get() = event.x

    val y: Double
        get() = event.y

    val button: Int
        get() = event.button()
}

class KeyInput(
    val event: KeyEvent,
) {
    val key: Int
        get() = event.key

    val modifiers: Int
        get() = event.modifiers
}

class CharInput(
    val event: CharacterEvent,
)

fun GuiEventListener.mouseClicked(input: MouseInput): Boolean = mouseClicked(input.event, input.doubled)

fun GuiEventListener.mouseReleased(input: MouseInput): Boolean = mouseReleased(input.event)

fun GuiEventListener.mouseDragged(
    input: MouseInput,
    dragX: Double,
    dragY: Double,
): Boolean = mouseDragged(input.event, dragX, dragY)

fun GuiEventListener.keyPressed(input: KeyInput): Boolean = keyPressed(input.event)

fun GuiEventListener.charTyped(input: CharInput): Boolean = charTyped(input.event)
*///?} else {
class MouseInput(
    val x: Double,
    val y: Double,
    val button: Int,
)

class KeyInput(
    val key: Int,
    val scanCode: Int,
    val modifiers: Int,
)

class CharInput(
    val codePoint: Char,
    val modifiers: Int,
)

fun GuiEventListener.mouseClicked(input: MouseInput): Boolean = mouseClicked(input.x, input.y, input.button)

fun GuiEventListener.mouseReleased(input: MouseInput): Boolean = mouseReleased(input.x, input.y, input.button)

fun GuiEventListener.mouseDragged(
    input: MouseInput,
    dragX: Double,
    dragY: Double,
): Boolean = mouseDragged(input.x, input.y, input.button, dragX, dragY)

fun GuiEventListener.keyPressed(input: KeyInput): Boolean = keyPressed(input.key, input.scanCode, input.modifiers)

fun GuiEventListener.charTyped(input: CharInput): Boolean = charTyped(input.codePoint, input.modifiers)
//?}
