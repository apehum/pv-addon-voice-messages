package dev.apehum.voicemessages.client.gui.render

import su.plo.lib.mod.client.render.gui.GuiRenderContext
import java.awt.Color
import kotlin.math.roundToInt
import kotlin.math.sqrt

fun roundedInset(
    row: Int,
    height: Int,
    radius: Int,
): Int {
    val center = row + 0.5
    val distance =
        when {
            center < radius -> radius - center
            center > height - radius -> center - (height - radius)
            else -> return 0
        }

    return (radius - sqrt(radius * radius - distance * distance)).roundToInt()
}

fun GuiRenderContext.fillRounded(
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
    radius: Int,
    color: Color,
) {
    val height = bottom - top

    for (row in 0 until height) {
        val inset = roundedInset(row, height, radius)
        fill(left + inset, top + row, right - inset, top + row + 1, color)
    }
}
