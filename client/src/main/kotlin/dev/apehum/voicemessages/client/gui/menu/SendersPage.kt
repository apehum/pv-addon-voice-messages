package dev.apehum.voicemessages.client.gui.menu

import com.mojang.blaze3d.platform.InputConstants
import dev.apehum.voicemessages.client.gui.chat.sendAndCloseChat
import dev.apehum.voicemessages.client.session.ClientSender
import dev.apehum.voicemessages.client.session.SendTarget
import dev.apehum.voicemessages.client.session.VoiceMessageSession
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import su.plo.lib.mod.client.render.Colors
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import kotlin.math.max

private const val MIN_SENDERS_WIDTH = 80
private const val SUBMENU_MARKER = ">"

class SendersPage(
    private val menu: MenuContext,
    private val session: VoiceMessageSession,
) : MenuPage() {
    private val font = Minecraft.getInstance().font

    private var highlighted = session.senders.indexOfFirst { it == session.target?.sender }

    override val children: List<AbstractWidget> =
        session.senders.mapIndexed { index, sender ->
            MenuRow(
                sender.displayName,
                color = if (sender == session.target?.sender) CURRENT_TARGET_COLOR else Colors.TEXT,
                marker = SUBMENU_MARKER.takeIf { sender.hasArguments },
                selected = { index == highlighted },
            ) { pick(sender) }
        }

    override fun renderWidget(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        GuiRenderContext(guiGraphics).fillFrame(this)
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick)
    }

    override fun measure(): MenuSize {
        val widest =
            session.senders.maxOfOrNull { sender ->
                val marker = if (sender.hasArguments) TEXT_PADDING + font.width(SUBMENU_MARKER) else 0
                font.width(sender.displayName) + marker
            } ?: 0

        return MenuSize(max(MIN_SENDERS_WIDTH, widest + TEXT_PADDING * 2), session.senders.size)
    }

    override fun arrange() {
        children.forEachIndexed { index, row -> placeRow(row, index) }
    }

    override fun handleKey(
        keyCode: Int,
        modifiers: Int,
    ): Boolean {
        val count = session.senders.size

        when (keyCode) {
            InputConstants.KEY_ESCAPE -> {
                menu.close()
            }

            InputConstants.KEY_UP -> {
                if (count > 0) highlighted = (max(highlighted, 0) - 1).mod(count)
            }

            InputConstants.KEY_DOWN -> {
                if (count > 0) highlighted = (highlighted + 1).mod(count)
            }

            InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
                session.senders.getOrNull(highlighted)?.let(::pick)
            }
        }

        return true
    }

    private fun pick(sender: ClientSender) {
        if (sender.hasArguments) return menu.showArguments(sender)

        menu.close()
        session.sendAndCloseChat(SendTarget(sender))
    }
}
