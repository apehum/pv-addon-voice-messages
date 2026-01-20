package dev.apehum.voicemessages.integration.paper

import dev.apehum.voicemessages.api.command.dsl.argument.CommandArgument
import dev.apehum.voicemessages.api.command.dsl.argument.StringReader
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.server.entity.player.McServerPlayer
import kotlin.text.startsWith

class PlayerNameArgument : CommandArgument<String> {
    override fun parse(
        source: McCommandSource,
        reader: StringReader,
    ): String {
        require(source is McServerPlayer)

        val playerName =
            reader
                .readString()
                .takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("Player name can't be empty")

        return playerName
    }

    override fun suggest(
        source: McCommandSource,
        reader: StringReader,
    ): List<String> {
        val playerName = reader.readString()

        return Bukkit
            .getOnlinePlayers()
            .filter {
                val player =
                    (source as? McServerPlayer)?.getInstance<Player>()
                        ?: return@filter false

                player.canSee(it)
            }.map { it.name }
            .filter { it.startsWith(playerName, ignoreCase = true) }
    }
}
