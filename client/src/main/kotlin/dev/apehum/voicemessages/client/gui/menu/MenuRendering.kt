package dev.apehum.voicemessages.client.gui.menu

import dev.apehum.voicemessages.client.gui.render.ACCENT_COLOR
import dev.apehum.voicemessages.client.gui.render.tint
import net.minecraft.client.gui.components.AbstractWidget
import su.plo.lib.mod.client.render.Colors
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import java.awt.Color

const val ITEM_HEIGHT = 17
const val TEXT_PADDING = 5
const val FONT_HEIGHT = 8

val ITEM_COLOR: Color = Colors.BLACK
val CURRENT_TARGET_COLOR = ACCENT_COLOR.tint(0.5f)

private val HOVER_COLOR = Color(0x323232)
private val FRAME_COLOR = Color(0x464646)
private val SCROLL_MARKER_COLOR = Color.WHITE

fun GuiRenderContext.fillFrame(frame: AbstractWidget) {
    fill(frame.x, frame.y, frame.right, frame.bottom, FRAME_COLOR)
}

fun GuiRenderContext.fillRow(
    left: Int,
    top: Int,
    right: Int,
    highlighted: Boolean,
) {
    fill(
        left + 1,
        top,
        right - 1,
        top + ITEM_HEIGHT - 1,
        if (highlighted) HOVER_COLOR else ITEM_COLOR,
    )
}

fun GuiRenderContext.fillScrollMarker(
    left: Int,
    right: Int,
    y: Int,
) {
    for (x in left until right step 2) {
        fill(x, y, x + 1, y + 1, SCROLL_MARKER_COLOR)
    }
}

fun textY(rowTop: Int): Int = rowTop + (ITEM_HEIGHT - 1 - FONT_HEIGHT) / 2 + 1
