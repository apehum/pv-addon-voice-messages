package dev.apehum.voicemessages.util.extension

import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.entity.player.McPlayer
import su.plo.slib.chat.AdventureComponentTextConverter
import su.plo.slib.libs.adventure.adventure.text.serializer.legacy.LegacyComponentSerializer

private val textConverter = AdventureComponentTextConverter()

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
