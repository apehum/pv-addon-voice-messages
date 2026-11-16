package dev.apehum.voicemessages.client.gui.chat

import com.mojang.blaze3d.platform.InputConstants
import dev.apehum.voicemessages.client.VoiceMessagesClient
import dev.apehum.voicemessages.client.gui.menu.SenderMenu
import dev.apehum.voicemessages.client.gui.render.ACCENT_COLOR
import dev.apehum.voicemessages.client.gui.render.tint
import dev.apehum.voicemessages.client.gui.widget.IconButton
import dev.apehum.voicemessages.client.gui.widget.MouseInput
import dev.apehum.voicemessages.client.modResource
import dev.apehum.voicemessages.client.session.SendTarget
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.network.chat.Component

val SEND_ICON = modResource("textures/gui/sprites/send.png")

val SEND_COLOR = ACCENT_COLOR.tint(0.3f)
private val SEND_HOVER_COLOR = ACCENT_COLOR.tint(0.6f)

class SendButton(
    x: Int,
    y: Int,
    size: Int,
    private val menu: SenderMenu,
) : IconButton(
        x,
        y,
        size,
        "pv.addon.voice_messages.chat.send",
        SEND_ICON,
        color = SEND_COLOR,
        hoverColor = SEND_HOVER_COLOR,
        onPress = { sendToTarget(menu, x + size, y) },
    ) {
    private var tooltipState: Pair<Boolean, SendTarget?>? = null

    override fun renderWidget(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        updateTooltip()
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick)
    }

    override fun handleMouseClicked(input: MouseInput): Boolean {
        if (input.button != InputConstants.MOUSE_BUTTON_RIGHT || !isMouseOver(input.x, input.y)) {
            return super.handleMouseClicked(input)
        }

        openMenu()
        return true
    }

    private fun openMenu() {
        playDownSound(Minecraft.getInstance().soundManager)
        menu.toggle(x + width, y)
    }

    private fun updateTooltip() {
        val target = VoiceMessagesClient.session?.target
        val state = menu.isOpen to target
        if (state == tooltipState) return

        tooltipState = state
        setTooltip(if (menu.isOpen) null else Tooltip.create(tooltipText(target)))
    }

    private fun tooltipText(target: SendTarget?): Component {
        val title =
            target
                ?.let { Component.translatable("pv.addon.voice_messages.chat.send.target", it.label()) }
                ?: message

        return Component
            .empty()
            .append(title)
            .append("\n")
            .append(Component.translatable("pv.addon.voice_messages.chat.send.hint").withStyle(ChatFormatting.GRAY))
    }

    private fun SendTarget.label(): Component =
        if (arguments.isEmpty()) {
            sender.displayName
        } else {
            Component.empty().append(sender.displayName).append(" $arguments")
        }
}

private fun sendToTarget(
    menu: SenderMenu,
    anchorRight: Int,
    anchorTop: Int,
) {
    val session = VoiceMessagesClient.session ?: return
    val target = session.target ?: return menu.toggle(anchorRight, anchorTop)

    session.send(target)
}
