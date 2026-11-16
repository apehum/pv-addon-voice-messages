package dev.apehum.voicemessages.client.gui.menu

import com.mojang.blaze3d.platform.InputConstants
import dev.apehum.voicemessages.channel.codec.SuggestResponse
import dev.apehum.voicemessages.client.gui.chat.SEND_COLOR
import dev.apehum.voicemessages.client.gui.chat.SEND_ICON
import dev.apehum.voicemessages.client.gui.render.DANGER_COLOR
import dev.apehum.voicemessages.client.session.ClientSender
import dev.apehum.voicemessages.client.session.SendTarget
import dev.apehum.voicemessages.client.session.VoiceMessageSession
import dev.apehum.voicemessages.client.session.failureText
import kotlinx.coroutines.Job
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.EditBox
import net.minecraft.network.chat.Component
import su.plo.lib.mod.client.Inputs
import su.plo.lib.mod.client.render.Colors
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import kotlin.math.max

private const val MIN_ARGUMENTS_WIDTH = 160
private const val MAX_INPUT_LENGTH = 256

class ArgumentsPage(
    private val menu: MenuContext,
    private val session: VoiceMessageSession,
    private val sender: ClientSender,
) : MenuPage() {
    private val font = Minecraft.getInstance().font

    private val input =
        EditBox(font, 0, 0, 0, FONT_HEIGHT, Component.empty()).apply {
            setBordered(false)
            setMaxLength(MAX_INPUT_LENGTH)
        }

    private val back =
        MenuRow(Component.literal("<"), centered = true) {
            menu.showSenders()
        }

    private val send =
        MenuIconButton(
            Component.translatable("pv.addon.voice_messages.chat.send"),
            SEND_ICON,
            SEND_COLOR,
        ) { submit() }

    private var lastValue: String? = null
    private var suggestions: SuggestionsList? = null
    private var lastSuggestions: Pair<String, SuggestResponse>? = null
    private var keepSuggestions = false
    private var suggestGeneration = 0
    private var suggestJob: Job? = null

    private var resolveJob: Job? = null
    private var error: Component? = null

    private var removed = false

    override val children: List<AbstractWidget>
        get() = listOfNotNull(suggestions, back, send, input)

    private val suggestionRows: Int
        get() = suggestions?.visibleCount ?: 0

    private val inputRow: Int
        get() = suggestionRows + if (error != null) 1 else 0

    init {
        input.value =
            session.target
                ?.takeIf { it.sender == sender }
                ?.arguments
                .orEmpty()
        input.moveCursorToEnd(false)
        input.isFocused = true
        input.setResponder(::onInputChanged)

        onInputChanged(input.value)
    }

    override fun renderWidget(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        val context = GuiRenderContext(guiGraphics)

        context.fillFrame(this)

        error?.let { error ->
            val top = rowTop(suggestionRows)
            context.fillRow(x, top, right, false)
            context.drawString(error.visualOrderText, x + TEXT_PADDING, textY(top), DANGER_COLOR)
        }

        context.fill(back.right, back.y, send.x, back.y + ITEM_HEIGHT - 1, ITEM_COLOR)

        if (input.value.isEmpty() && suggestions == null) {
            sender.argumentsHint?.let {
                context.drawString(it.visualOrderText, input.x, input.y, Colors.GRAY)
            }
        }

        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick)
    }

    override fun measure(): MenuSize {
        val widestSuggestion = suggestions?.items?.maxOf(font::width) ?: 0
        val errorWidth = error?.let(font::width) ?: 0

        return MenuSize(
            max(MIN_ARGUMENTS_WIDTH, max(widestSuggestion, errorWidth) + TEXT_PADDING * 2),
            inputRow + 1,
        )
    }

    override fun arrange() {
        suggestions?.setRectangle(width, suggestionRows * ITEM_HEIGHT, x, rowTop(0))

        val inputTop = rowTop(inputRow)
        back.setRectangle(ITEM_HEIGHT, ITEM_HEIGHT, x, inputTop)
        send.setRectangle(ITEM_HEIGHT, ITEM_HEIGHT, right - ITEM_HEIGHT, inputTop)

        input.x = back.right + TEXT_PADDING
        input.y = textY(inputTop)

        val inputWidth = send.x - TEXT_PADDING - input.x
        if (input.width != inputWidth) {
            input.width = inputWidth
            input.setCursorPosition(input.cursorPosition)
        }
    }

    override fun removed() {
        removed = true
        input.isFocused = false
        suggestJob?.cancel()
        resolveJob?.cancel()
    }

    override fun handleKey(
        keyCode: Int,
        modifiers: Int,
    ): Boolean {
        val suggestions = suggestions

        return when (keyCode) {
            InputConstants.KEY_ESCAPE -> {
                if (suggestions != null) hideSuggestions() else menu.close()
                true
            }

            InputConstants.KEY_UP -> {
                suggestions?.let { cycleSuggestion(it, -1) }
                true
            }

            InputConstants.KEY_DOWN -> {
                suggestions?.let { cycleSuggestion(it, 1) }
                true
            }

            InputConstants.KEY_TAB -> {
                if (suggestions == null) {
                    showLastSuggestions()
                } else {
                    val shift = Inputs.hasShiftDown(modifiers)
                    if (suggestions.tabCycles) {
                        suggestions.cycle(if (shift) -1 else 1)
                    }

                    useSuggestion(suggestions)
                }

                true
            }

            InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
                submit()
                true
            }

            InputConstants.KEY_BACKSPACE -> {
                val backToSenders = input.value.isEmpty()
                if (backToSenders) menu.showSenders()

                backToSenders
            }

            else -> {
                false
            }
        }
    }

    private fun submit() {
        if (resolveJob?.isActive == true) return

        val target = SendTarget(sender, input.value.trim())

        resolveJob =
            session.resolve(target) { response ->
                if (removed) return@resolve

                val failure = response.failure
                if (failure != null) {
                    error = failureText(failure, response.message)
                    return@resolve
                }

                menu.close()
                session.send(target)
            }
    }

    private fun onInputChanged(value: String) {
        if (value == lastValue) return
        lastValue = value

        error = null
        if (keepSuggestions) return

        hideSuggestions()
        requestSuggestions(value)
    }

    private fun requestSuggestions(value: String) {
        suggestJob?.cancel()

        val generation = ++suggestGeneration
        suggestJob =
            session.suggest(sender, value) { response ->
                if (removed || generation != suggestGeneration) return@suggest

                lastSuggestions = value to response
                showLastSuggestions()
            }
    }

    private fun showLastSuggestions() {
        val (value, response) = lastSuggestions ?: return
        if (value != input.value || response.suggestions.isEmpty()) return

        suggestions = SuggestionsList(value, response, onHover = ::refreshInputSuggestion, onUse = ::useSuggestion)
        refreshInputSuggestion()
    }

    private fun hideSuggestions() {
        suggestions = null
        input.setSuggestion(null)
    }

    private fun cycleSuggestion(
        suggestions: SuggestionsList,
        delta: Int,
    ) {
        suggestions.cycle(delta, tabCycles = false)
        refreshInputSuggestion()
    }

    private fun useSuggestion(suggestions: SuggestionsList) {
        val cursor = suggestions.cursorAfterUse

        keepSuggestions = true
        input.value = suggestions.applied
        input.cursorPosition = cursor
        input.setHighlightPos(cursor)
        keepSuggestions = false

        suggestions.markUsed()
        refreshInputSuggestion()
    }

    private fun refreshInputSuggestion() {
        val applied = suggestions?.applied
        input.setSuggestion(applied?.takeIf { it.startsWith(input.value) }?.substring(input.value.length))
    }
}
