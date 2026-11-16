package dev.apehum.voicemessages.client.gui.menu

import dev.apehum.voicemessages.client.gui.widget.CompositeWidget
import dev.apehum.voicemessages.client.gui.widget.KeyInput
import dev.apehum.voicemessages.client.session.ClientSender
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.network.chat.Component

interface MenuContext {
    fun showSenders()

    fun showArguments(sender: ClientSender)

    fun close()
}

class MenuSize(
    val contentWidth: Int,
    val rows: Int,
)

abstract class MenuPage : CompositeWidget(0, 0, 0, 0, Component.empty()) {
    override fun handleKeyPressed(input: KeyInput): Boolean =
        handleKey(input.key, input.modifiers) ||
            super.handleKeyPressed(input)

    abstract fun measure(): MenuSize

    abstract fun arrange()

    open fun removed() = Unit

    protected abstract fun handleKey(
        keyCode: Int,
        modifiers: Int,
    ): Boolean

    protected fun rowTop(row: Int): Int = y + 1 + row * ITEM_HEIGHT

    protected fun placeRow(
        widget: AbstractWidget,
        row: Int,
    ) {
        widget.setRectangle(width, ITEM_HEIGHT, x, rowTop(row))
    }
}
