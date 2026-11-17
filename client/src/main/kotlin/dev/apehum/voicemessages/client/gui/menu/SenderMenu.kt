package dev.apehum.voicemessages.client.gui.menu

import dev.apehum.voicemessages.client.VoiceMessagesClient
import dev.apehum.voicemessages.client.gui.widget.AbstractWidget
import dev.apehum.voicemessages.client.gui.widget.CharInput
import dev.apehum.voicemessages.client.gui.widget.KeyInput
import dev.apehum.voicemessages.client.gui.widget.MouseInput
import dev.apehum.voicemessages.client.gui.widget.charTyped
import dev.apehum.voicemessages.client.gui.widget.keyPressed
import dev.apehum.voicemessages.client.gui.widget.mouseClicked
import dev.apehum.voicemessages.client.session.ClientSender
import dev.apehum.voicemessages.client.session.VoiceMessageSession
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import kotlin.math.min

private const val SCREEN_MARGIN = 2
private const val ANCHOR_GAP = 1

class SenderMenu : AbstractWidget(0, 0, 0, 0, Component.empty()) {
    private var page: MenuPage? = null

    private var anchorRight = 0
    private var anchorTop = 0

    private val context =
        object : MenuContext {
            override fun showSenders() {
                val session = session() ?: return
                open(SendersPage(this, session))
            }

            override fun showArguments(sender: ClientSender) {
                val session = session() ?: return
                open(ArgumentsPage(this, session, sender))
            }

            override fun close() = this@SenderMenu.close()
        }

    val isOpen: Boolean
        get() = page != null

    init {
        visible = false
    }

    override fun renderWidget(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        if (session() == null) return close()
        val page = page ?: return

        //? if <1.21.6 {
        guiGraphics.pose().pushPose()
        guiGraphics.pose().translate(0f, 0f, 200f)
        //?} else {
        /*guiGraphics.nextStratum()
        *///?}

        place(page)
        //~ if >=26.1 'render' -> 'extractRenderState'
        page.render(guiGraphics, mouseX, mouseY, partialTick)

        //? if <1.21.6
        guiGraphics.pose().popPose()
    }

    override fun handleMouseClicked(input: MouseInput): Boolean {
        val page = page ?: return false

        if (session() != null && page.isMouseOver(input.x, input.y)) {
            page.mouseClicked(input)
        } else {
            close()
        }

        return true
    }

    override fun handleKeyPressed(input: KeyInput): Boolean {
        val page = page ?: return false

        page.keyPressed(input)
        return true
    }

    override fun handleCharTyped(input: CharInput): Boolean {
        val page = page ?: return false

        page.charTyped(input)
        return true
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        scrollX: Double,
        scrollY: Double,
    ): Boolean {
        val page = page ?: return false

        page.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
        return true
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) = Unit

    fun toggle(
        anchorRight: Int,
        anchorTop: Int,
    ) {
        if (isOpen) return close()

        this.anchorRight = anchorRight
        this.anchorTop = anchorTop
        context.showSenders()
    }

    fun close() {
        val page = page ?: return

        this.page = null
        visible = false
        page.removed()
    }

    private fun open(page: MenuPage) {
        this.page?.removed()
        this.page = page
        visible = true
        place(page)
    }

    private fun place(page: MenuPage) {
        val size = page.measure()
        val screenWidth = Minecraft.getInstance().window.guiScaledWidth
        val pageWidth = min(size.contentWidth, screenWidth - SCREEN_MARGIN * 2)
        val pageHeight = size.rows * ITEM_HEIGHT + 1

        page.setRectangle(
            pageWidth,
            pageHeight,
            (anchorRight - pageWidth).coerceAtLeast(SCREEN_MARGIN),
            (anchorTop - ANCHOR_GAP - pageHeight).coerceAtLeast(SCREEN_MARGIN),
        )
        page.arrange()
    }

    private fun session(): VoiceMessageSession? = VoiceMessagesClient.session?.takeIf { it.recorder.draft != null }
}
