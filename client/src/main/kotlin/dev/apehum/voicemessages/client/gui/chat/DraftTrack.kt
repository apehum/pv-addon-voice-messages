package dev.apehum.voicemessages.client.gui.chat

import dev.apehum.voicemessages.client.VoiceMessagesClient
import dev.apehum.voicemessages.client.gui.render.ACCENT_COLOR
import dev.apehum.voicemessages.client.gui.render.fillRounded
import dev.apehum.voicemessages.client.gui.render.roundedInset
import dev.apehum.voicemessages.client.gui.render.shade
import dev.apehum.voicemessages.client.gui.render.tint
import dev.apehum.voicemessages.client.modResource
import dev.apehum.voicemessages.client.record.VoiceMessageDraft
import dev.apehum.voicemessages.client.record.VoiceMessageDraftPlayer
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import su.plo.lib.mod.client.render.RenderUtil
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import java.awt.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration

//? if >=1.21.11
/*import net.minecraft.client.input.MouseButtonEvent*/

private const val DRAFT_KEY = "pv.addon.voice_messages.chat.draft"

private val PLAY_ICON = modResource("textures/gui/sprites/play.png")
private val PAUSE_ICON = modResource("textures/gui/sprites/pause.png")

private const val HANDLE_WIDTH = 3
private const val HANDLE_GRAB = 2
private const val GRIP_INSET = 3

private const val BAR_WIDTH = 1
private const val BAR_PITCH = 2
private const val BAR_INSET = 1

private const val CHIP_PADDING = 3
private const val CHIP_GAP = 3
private const val GLYPH_WIDTH = 3
private const val GLYPH_HEIGHT = 5

// todo: why it's hard-coded? shouldn't it be resolved from actual mc font?
private const val FONT_HEIGHT = 8

private val TRACK_COLOR = ACCENT_COLOR.shade(0.35f)
private val TRIMMED_COLOR = TRACK_COLOR.shade(0.4f)

// todo: i am still not sure about this color; right now it's just pv's accent color
private val PLAYED_COLOR = ACCENT_COLOR

private val BAR_COLOR = Color(255, 255, 255, 140)
private val PLAYED_BAR_COLOR = Color.WHITE
private val TRIMMED_BAR_COLOR = Color(255, 255, 255, 60)

private val HANDLE_COLOR = ACCENT_COLOR.tint(0.45f)
private val GRIP_COLOR = Color(255, 255, 255, 210)
private val CHIP_COLOR = ACCENT_COLOR.tint(0.35f)
private val GLYPH_COLOR = Color.WHITE

class DraftTrack(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
) : AbstractWidget(x, y, width, height, Component.empty()) {
    private enum class Handle { START, END }

    private var dragging: Handle? = null

    private val waveLeft get() = x + HANDLE_WIDTH
    private val waveWidth get() = width - HANDLE_WIDTH * 2

    private val radius get() = height / 2

    override fun renderWidget(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        val session = VoiceMessagesClient.session ?: return
        val draft = session.recorder.draft ?: return
        val preview = session.preview
        val context = GuiRenderContext(guiGraphics)

        val trimLeft = draft.frameX(draft.trimStart)
        val trimRight = draft.frameX(draft.trimEnd)
        val playhead = draft.frameX(draft.playheadFrame(preview))

        renderTrack(context, trimLeft, trimRight, playhead)
        renderBars(context, draft, trimLeft, trimRight, playhead)
        renderHandle(context, trimLeft - HANDLE_WIDTH)
        renderHandle(context, trimRight)
        renderChip(context, draft, preview)
    }

    //? if >=1.21.11 {
    /*override fun onClick(
        event: MouseButtonEvent,
        doubled: Boolean,
    ) = press(event.x)

    override fun onDrag(
        event: MouseButtonEvent,
        dragX: Double,
        dragY: Double,
    ) = drag(event.x)

    override fun onRelease(event: MouseButtonEvent) = release()
    *///?} else {
    override fun onClick(
        mouseX: Double,
        mouseY: Double,
    ) = press(mouseX)

    override fun onDrag(
        mouseX: Double,
        mouseY: Double,
        dragX: Double,
        dragY: Double,
    ) = drag(mouseX)

    override fun onRelease(
        mouseX: Double,
        mouseY: Double,
    ) = release()
    //?}

    private fun press(mouseX: Double) {
        val session = VoiceMessagesClient.session ?: return
        val draft = session.recorder.draft ?: return
        val preview = session.preview

        dragging = draft.grabbedHandle(mouseX)
        if (dragging != null) {
            preview.stop()
            return
        }

        val chip = draft.chip(preview)
        if (chip != null && mouseX >= chip.left && mouseX < chip.left + chip.width) {
            preview.toggle(draft)
            return
        }

        val frame = draft.frameAt(mouseX).coerceIn(draft.trimStart, draft.trimEnd)
        preview.play(draft, frame - draft.trimStart)
    }

    private fun drag(mouseX: Double) {
        val draft = VoiceMessagesClient.session?.recorder?.draft ?: return
        val frame = draft.frameAt(mouseX)

        when (dragging) {
            Handle.START -> draft.trimStartTo(frame)
            Handle.END -> draft.trimEndTo(frame)
            null -> Unit
        }
    }

    private fun release() {
        dragging = null
    }

    override fun updateWidgetNarration(output: NarrationElementOutput) {
        val duration = VoiceMessagesClient.session?.recorder?.draft?.trimmedDuration ?: Duration.ZERO
        output.add(NarratedElementType.TITLE, Component.translatable(DRAFT_KEY, duration.formatted()))
    }

    private fun renderTrack(
        context: GuiRenderContext,
        trimLeft: Int,
        trimRight: Int,
        playhead: Int,
    ) {
        for (row in 0 until height) {
            val inset = roundedInset(row, height, radius)
            val rowLeft = x + inset
            val rowRight = x + width - inset
            val top = y + row

            context.fill(rowLeft, top, rowRight, top + 1, TRIMMED_COLOR)
            context.fillSpan(rowLeft, rowRight, top, trimLeft, trimRight, TRACK_COLOR)
            context.fillSpan(rowLeft, rowRight, top, trimLeft, min(playhead, trimRight), PLAYED_COLOR)
        }
    }

    private fun renderBars(
        context: GuiRenderContext,
        draft: VoiceMessageDraft,
        trimLeft: Int,
        trimRight: Int,
        playhead: Int,
    ) {
        val bars = waveWidth / BAR_PITCH
        if (bars <= 0) return

        val center = y + height / 2
        val maxReach = (height - BAR_INSET * 2) / 2

        draft.waveform(bars).forEachIndexed { index, level ->
            val barLeft = waveLeft + index * BAR_PITCH
            val reach = (level * maxReach).roundToInt().coerceIn(1, maxReach)

            val color =
                when {
                    barLeft < trimLeft || barLeft >= trimRight -> TRIMMED_BAR_COLOR
                    barLeft < playhead -> PLAYED_BAR_COLOR
                    else -> BAR_COLOR
                }

            context.fill(barLeft, center - reach, barLeft + BAR_WIDTH, center + reach, color)
        }
    }

    private fun renderHandle(
        context: GuiRenderContext,
        handleX: Int,
    ) {
        for (row in 0 until height) {
            val inset = roundedInset(row, height, radius)
            context.fillSpan(x + inset, x + width - inset, y + row, handleX, handleX + HANDLE_WIDTH, HANDLE_COLOR)
        }

        context.fill(handleX + 1, y + GRIP_INSET, handleX + 2, y + height - GRIP_INSET, GRIP_COLOR)
    }

    private fun renderChip(
        context: GuiRenderContext,
        draft: VoiceMessageDraft,
        preview: VoiceMessageDraftPlayer,
    ) {
        val chip = draft.chip(preview) ?: return
        val chipHeight = height - 2
        val top = y + 1

        context.fillRounded(chip.left, top, chip.left + chip.width, top + chipHeight, chipHeight / 2, CHIP_COLOR)

        context.blitIcon(
            if (preview.isPlaying) PAUSE_ICON else PLAY_ICON,
            chip.left + CHIP_PADDING,
            top + (chipHeight - GLYPH_HEIGHT) / 2,
            GLYPH_WIDTH,
            GLYPH_HEIGHT,
            GLYPH_COLOR,
        )

        val label = chip.label ?: return
        context.drawString(
            label,
            chip.left + CHIP_PADDING + GLYPH_WIDTH + CHIP_GAP,
            top + (chipHeight - FONT_HEIGHT) / 2,
            Color.WHITE,
        )
    }

    private fun VoiceMessageDraft.frameX(frame: Int): Int = waveLeft + (waveWidth.toDouble() * frame / frames.size).roundToInt()

    private fun VoiceMessageDraft.frameAt(mouseX: Double): Int =
        ((mouseX - waveLeft) / waveWidth * frames.size).roundToInt().coerceIn(0, frames.size)

    private fun VoiceMessageDraft.playheadFrame(preview: VoiceMessageDraftPlayer): Int =
        trimStart + ((trimEnd - trimStart) * preview.progress(this)).roundToInt()

    private fun VoiceMessageDraft.grabbedHandle(mouseX: Double): Handle? {
        val trimLeft = frameX(trimStart)
        val trimRight = frameX(trimEnd)

        return when {
            mouseX >= trimLeft - HANDLE_WIDTH - HANDLE_GRAB && mouseX < trimLeft + HANDLE_GRAB -> Handle.START
            mouseX >= trimRight - HANDLE_GRAB && mouseX < trimRight + HANDLE_WIDTH + HANDLE_GRAB -> Handle.END
            else -> null
        }
    }

    private fun VoiceMessageDraft.chip(preview: VoiceMessageDraftPlayer): Chip? {
        val trimLeft = frameX(trimStart)
        val trimRight = frameX(trimEnd)
        val room = trimRight - trimLeft - 2

        val label = chipLabel(preview)
        val labelled = CHIP_PADDING * 2 + GLYPH_WIDTH + CHIP_GAP + RenderUtil.getStringWidth(label)
        val bare = CHIP_PADDING * 2 + GLYPH_WIDTH

        val chipWidth =
            when {
                room >= labelled -> labelled
                room >= bare -> bare
                else -> return null
            }

        return Chip(trimLeft + 1 + (room - chipWidth) / 2, chipWidth, label.takeIf { chipWidth == labelled })
    }

    private fun VoiceMessageDraft.chipLabel(preview: VoiceMessageDraftPlayer): String =
        if (preview.isPlaying) {
            (trimmedDuration * preview.progress(this)).formatted()
        } else {
            trimmedDuration.formatted()
        }
}

private data class Chip(
    val left: Int,
    val width: Int,
    val label: String?,
)

private fun GuiRenderContext.fillSpan(
    rowLeft: Int,
    rowRight: Int,
    top: Int,
    left: Int,
    right: Int,
    color: Color,
) {
    val from = max(rowLeft, left)
    val to = min(rowRight, right)
    if (from >= to) return

    fill(from, top, to, top + 1, color)
}

private fun Duration.formatted(): String = "%d:%02d".format(inWholeMinutes, inWholeSeconds % 60)
