package dev.apehum.voicemessages.client.gui.widget

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import java.awt.Color

//? if >=1.21.11 {
/*import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
*///?}

private const val ICON_SIZE = 8

private val ICON_IDLE_COLOR = Color(187, 187, 187)

private val BUTTON_SPRITE = ResourceLocation.withDefaultNamespace("widget/button")
private val BUTTON_HOVER_SPRITE = ResourceLocation.withDefaultNamespace("widget/button_highlighted")

open class IconButton(
    x: Int,
    y: Int,
    size: Int,
    translationKey: String,
    private val icon: ResourceLocation,
    private val iconSize: Int = ICON_SIZE,
    private val iconOffset: Pair<Int, Int> = 0 to 0,
    private val color: Color = ICON_IDLE_COLOR,
    private val hoverColor: Color = Color.WHITE,
    private val background: Boolean = false,
    private val onPress: () -> Unit,
) : AbstractWidget(x, y, size, size, Component.translatable(translationKey)) {
    init {
        setTooltip(Tooltip.create(Component.translatable(translationKey)))
    }

    override fun renderWidget(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        if (background) {
            val sprite = if (isHoveredOrFocused) BUTTON_HOVER_SPRITE else BUTTON_SPRITE

            //? if >=1.21.11 {
            /*guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height)
            *///?} else {
            guiGraphics.blitSprite(sprite, x, y, width, height)
            //?}
        }

        val offset = (width - iconSize) / 2

        GuiRenderContext(guiGraphics).blitIcon(
            icon,
            x + offset + iconOffset.first,
            y + offset + iconOffset.second,
            iconSize,
            iconSize,
            if (isHovered) hoverColor else color,
        )
    }

    //? if >=1.21.11 {
    /*override fun onClick(
        event: MouseButtonEvent,
        doubled: Boolean,
    ) {
        onPress()
    }
    *///?} else {
    override fun onClick(
        mouseX: Double,
        mouseY: Double,
    ) {
        onPress()
    }
    //?}

    override fun updateWidgetNarration(output: NarrationElementOutput) {
        output.add(NarratedElementType.TITLE, message)
    }
}
