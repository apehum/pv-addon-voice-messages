package dev.apehum.voicemessages.integration.paper

import dev.apehum.voicemessages.api.VoiceMessagesAPIProvider
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.entity.Player
import java.util.UUID

fun createVoiceMessageReplacement(player: Player): TextReplacementConfig {
    val voiceMessages = VoiceMessagesAPIProvider.getInstance()

    return TextReplacementConfig
        .builder()
        .match("\\[vm:([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})]")
        .once()
        .replacement { match, builder ->
            val voiceMessageId = UUID.fromString(match.group(1))
            val voiceMessage =
                runBlocking { voiceMessages.messageStorage.getById(voiceMessageId) }
                    ?: return@replacement builder.build()

            val json = voiceMessages.formatVoiceMessageToJson(voiceMessage, player.locale)
            val voiceMessageComponent = GsonComponentSerializer.gson().deserialize(json)

            voiceMessageComponent
        }.build()
}
