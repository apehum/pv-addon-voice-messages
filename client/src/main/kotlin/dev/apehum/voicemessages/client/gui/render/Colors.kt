package dev.apehum.voicemessages.client.gui.render

import java.awt.Color
import kotlin.math.roundToInt

val ACCENT_COLOR = Color(24, 110, 240)
val DANGER_COLOR = Color(255, 85, 85)

fun Color.tint(factor: Float): Color = mix(255, factor)

fun Color.shade(factor: Float): Color = mix(0, factor)

private fun Color.mix(
    target: Int,
    factor: Float,
): Color =
    Color(
        red + ((target - red) * factor).roundToInt(),
        green + ((target - green) * factor).roundToInt(),
        blue + ((target - blue) * factor).roundToInt(),
        alpha,
    )
