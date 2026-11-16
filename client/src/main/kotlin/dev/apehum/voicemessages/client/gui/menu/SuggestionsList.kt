package dev.apehum.voicemessages.client.gui.menu

import dev.apehum.voicemessages.channel.codec.SuggestResponse
import dev.apehum.voicemessages.client.gui.widget.AbstractWidget
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.sounds.SoundManager
import net.minecraft.network.chat.Component
import su.plo.lib.mod.client.render.gui.GuiCursor
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

private const val MAX_VISIBLE_SUGGESTIONS = 8
private const val SUGGESTION_LINE_START_OFFSET = 1

private val SUGGESTION_COLOR = Color(0xAAAAAA)
private val SELECTED_SUGGESTION_COLOR = CURRENT_TARGET_COLOR

class SuggestionsList(
    private val originalContents: String,
    private val response: SuggestResponse,
    private val onHover: () -> Unit,
    private val onUse: (SuggestionsList) -> Unit,
) : AbstractWidget(0, 0, 0, 0, Component.empty()) {
    var tabCycles = false
        private set

    private var offset = 0
    private var current = 0

    private var lastMouseX = Int.MIN_VALUE
    private var lastMouseY = Int.MIN_VALUE

    val items: List<String>
        get() = response.suggestions

    val visibleCount: Int
        get() = min(items.size, MAX_VISIBLE_SUGGESTIONS)

    val applied: String
        get() = originalContents.take(response.replaceFrom) + items[current]

    val cursorAfterUse: Int
        get() = response.replaceFrom + items[current].length

    private val canScrollUp: Boolean
        get() = offset > 0

    private val canScrollDown: Boolean
        get() = items.size > offset + visibleCount

    private val maxOffset: Int
        get() = max(items.size - MAX_VISIBLE_SUGGESTIONS, 0)

    override fun renderWidget(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        val context = GuiRenderContext(guiGraphics)

        val hoveredRow = rowAt(mouseY.toDouble()).takeIf { isHovered }
        if (hover(hoveredRow, mouseX, mouseY)) onHover()
        if (hoveredRow != null) context.requestCursor(GuiCursor.POINTING_HAND)

        for (row in 0 until visibleCount) {
            val index = offset + row
            val top = y + row * ITEM_HEIGHT

            context.fillRow(x, top, right, false)
            context.drawString(
                items[index],
                x + TEXT_PADDING,
                textY(top),
                if (index == current) SELECTED_SUGGESTION_COLOR else SUGGESTION_COLOR,
            )
        }

        if (canScrollUp) context.fillScrollMarker(x, right, y - 1)
        if (canScrollDown) context.fillScrollMarker(x, right, bottom - 1)
    }

    override fun handleClick(
        mouseX: Double,
        mouseY: Double,
    ) {
        use(mouseY)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        scrollX: Double,
        scrollY: Double,
    ): Boolean {
        offset = (offset - scrollY.coerceIn(-1.0, 1.0)).toInt().coerceIn(0, maxOffset)
        return true
    }

    override fun playDownSound(soundManager: SoundManager) = Unit

    override fun updateWidgetNarration(output: NarrationElementOutput) {
        output.add(NarratedElementType.TITLE, Component.literal(items[current]))
    }

    fun cycle(
        delta: Int,
        tabCycles: Boolean = this.tabCycles,
    ) {
        this.tabCycles = tabCycles
        select(current + delta)

        if (current < offset) {
            offset = current.coerceIn(0, maxOffset)
        } else if (current > offset + MAX_VISIBLE_SUGGESTIONS - 1) {
            offset = (current + SUGGESTION_LINE_START_OFFSET - MAX_VISIBLE_SUGGESTIONS).coerceIn(0, maxOffset)
        }
    }

    fun markUsed() {
        tabCycles = true
    }

    private fun hover(
        row: Int?,
        mouseX: Int,
        mouseY: Int,
    ): Boolean {
        val moved = mouseX != lastMouseX || mouseY != lastMouseY
        lastMouseX = mouseX
        lastMouseY = mouseY

        if (!moved || row == null) return false
        select(offset + row)
        return true
    }

    private fun use(mouseY: Double) {
        select(offset + rowAt(mouseY))
        onUse(this)
    }

    private fun select(index: Int) {
        current = index.mod(items.size)
    }

    private fun rowAt(mouseY: Double): Int = ((mouseY - y) / ITEM_HEIGHT).toInt()
}
