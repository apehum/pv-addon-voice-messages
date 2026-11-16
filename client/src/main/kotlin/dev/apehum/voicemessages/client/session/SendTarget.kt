package dev.apehum.voicemessages.client.session

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import dev.apehum.voicemessages.channel.codec.SenderInfo
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization

class ClientSender(
    val name: String,
    val displayName: Component,
    val argumentsHint: Component?,
) {
    val hasArguments: Boolean
        get() = argumentsHint != null
}

data class SendTarget(
    val sender: ClientSender,
    val arguments: String = "",
)

fun SenderInfo.toClientSender(): ClientSender {
    val displayName =
        displayNameJson?.let(::parseComponent)
            ?: Component.translatableWithFallback("pv.addon.voice_messages.sender.$name", name)

    val argumentsHint =
        arguments
            .takeIf { it.isNotEmpty() }
            ?.map { argument ->
                val argumentName = argument.displayNameJson?.let(::parseComponent) ?: Component.literal(argument.name)
                Component.literal("<").append(argumentName).append(">")
            }?.reduce { hint, argument -> hint.append(" ").append(argument) }

    return ClientSender(name, displayName, argumentsHint)
}

private fun parseComponent(json: String): Component? {
    val ops =
        Minecraft
            .getInstance()
            .connection
            ?.registryAccess()
            ?.createSerializationContext(JsonOps.INSTANCE)
            ?: JsonOps.INSTANCE

    return try {
        ComponentSerialization.CODEC
            .parse(ops, JsonParser.parseString(json))
            .result()
            .orElse(null)
    } catch (_: Exception) {
        null
    }
}
