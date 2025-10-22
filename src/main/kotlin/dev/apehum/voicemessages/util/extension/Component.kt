package dev.apehum.voicemessages.util.extension

import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.chat.style.McTextClickEvent
import su.plo.slib.api.chat.style.McTextHoverEvent
import su.plo.slib.api.chat.style.McTextStyle
import su.plo.slib.api.entity.player.McPlayer
import su.plo.slib.chat.AdventureComponentTextConverter
import su.plo.slib.libs.adventure.adventure.text.Component
import su.plo.slib.libs.adventure.adventure.text.TextComponent
import su.plo.slib.libs.adventure.adventure.text.TranslatableComponent
import su.plo.slib.libs.adventure.adventure.text.event.HoverEvent
import su.plo.slib.libs.adventure.adventure.text.format.NamedTextColor
import su.plo.slib.libs.adventure.adventure.text.format.TextDecoration
import su.plo.slib.libs.adventure.adventure.text.serializer.legacy.LegacyComponentSerializer

private val textConverter = AdventureComponentTextConverter()

fun McTextComponent.toAdventure() = textConverter.convert(this)

fun Component.toMc(): McTextComponent {
    val component =
        when (this) {
            is TranslatableComponent -> {
                McTextComponent
                    .translatable(
                        key(),
                        *arguments()
                            .map {
                                val value = it.value()
                                if (value is Component) {
                                    value.toMc()
                                } else {
                                    value
                                }
                            }.toTypedArray(),
                    )
            }

            is TextComponent -> McTextComponent.literal(content())

            else -> throw IllegalArgumentException("Unsupported component type: ${this.javaClass}")
        }

    // apply styles
    component.withStyle(*getMcStyles().toTypedArray())

    // apply click event
    component.clickEvent(getMcClickEvent())

    // apply hover event
    component.hoverEvent(getMcHoverEvent())

    // add siblings
    component.append(children().map { it.toMc() })

    return component
}

private fun Component.getMcClickEvent(): McTextClickEvent? =
    clickEvent()?.let {
        McTextClickEvent.clickEvent(
            McTextClickEvent.Action.valueOf(it.action().name),
            it.value(),
        )
    }

private fun Component.getMcHoverEvent(): McTextHoverEvent? =
    hoverEvent()?.let {
        when (it.action()) {
            HoverEvent.Action.SHOW_TEXT -> {
                McTextHoverEvent.showText((it.value() as Component).toMc())
            }

            else -> null
        }
    }

private fun Component.getMcStyles(): List<McTextStyle> {
    val textColor = style().color() as? NamedTextColor
    val mcTextColor = textColor?.toString()?.let { McTextStyle.valueOf(it.uppercase()) }

    val mcTextDecorations =
        style()
            .decorations()
            .filter { it.value == TextDecoration.State.TRUE }
            .keys
            .map {
                McTextStyle.valueOf(it.name)
            }

    val styles = mutableListOf<McTextStyle>()
    if (mcTextColor != null) styles.add(mcTextColor)

    styles.addAll(mcTextDecorations)

    return styles
}

fun McTextComponent.toLegacyString() = LegacyComponentSerializer.legacySection().serialize(textConverter.convert(this))

fun McPlayer.sendTranslatable(
    key: String,
    vararg arguments: Any,
) {
    val argumentsComponents = arguments.asComponents()
    sendMessage(McTextComponent.translatable(key, *argumentsComponents))
}

fun McPlayer.sendTranslatableActionbar(
    key: String,
    vararg arguments: Any,
) {
    val argumentsComponents = arguments.asComponents()
    sendActionBar(McTextComponent.translatable(key, *argumentsComponents))
}

private fun Array<out Any>.asComponents() =
    map {
        if (it is McTextComponent) {
            it
        } else {
            McTextComponent.literal(it.toString())
        }
    }.toTypedArray()

fun Collection<McTextComponent>.merge(): McTextComponent = McTextComponent.empty().append(this)
