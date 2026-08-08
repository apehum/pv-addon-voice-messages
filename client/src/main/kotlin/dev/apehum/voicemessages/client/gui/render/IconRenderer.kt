package dev.apehum.voicemessages.client.gui.render

import net.minecraft.resources.ResourceLocation
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import su.plo.lib.mod.client.render.pipeline.RenderPipeline
import su.plo.lib.mod.client.render.pipeline.RenderPipelines
import su.plo.lib.mod.client.render.shader.SolidColorShader
import java.awt.Color

const val ICON_SIZE = 8

const val SHADOW_OFFSET = 1
private const val SHADOW_SHADE = 0.75f

fun GuiRenderContext.blitIcon(
    icon: ResourceLocation,
    x: Int,
    y: Int,
    size: Int,
    color: Color,
    shadow: Boolean = true,
) = blitIcon(icon, x, y, size, size, color, shadow)

fun GuiRenderContext.blitIcon(
    icon: ResourceLocation,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    color: Color,
    shadow: Boolean = true,
) {
    if (shadow && SolidColorShader.isAvailable()) {
        blitScaled(
            icon,
            x + SHADOW_OFFSET,
            y + SHADOW_OFFSET,
            width,
            height,
            color.shadow(),
            RenderPipelines.GUI_TEXTURE_SOLID_COLOR,
        )
    }

    blitScaled(icon, x, y, width, height, color, RenderPipelines.GUI_TEXTURE_COLOR)
}

private fun GuiRenderContext.blitScaled(
    icon: ResourceLocation,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    color: Color,
    pipeline: RenderPipeline,
) {
    blitColor(icon, x, x + width, y, y + height, 0f, 1f, 0f, 1f, color, pipeline)
}

fun Color.shadow(): Color = shade(SHADOW_SHADE)
