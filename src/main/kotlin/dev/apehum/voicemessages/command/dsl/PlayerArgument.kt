package dev.apehum.voicemessages.command.dsl

import dev.apehum.voicemessages.command.dsl.argument.CommandArgument
import dev.apehum.voicemessages.command.dsl.argument.StringReader
import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.server.McServerLib
import su.plo.slib.api.server.entity.player.McServerPlayer

class PlayerArgument(
    private val minecraftServer: McServerLib,
) : CommandArgument<McServerPlayer> {
    override fun parse(
        source: McCommandSource,
        reader: StringReader,
    ): McServerPlayer {
        require(source is McServerPlayer)

        val playerName =
            reader
                .readString()
                .takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("Player name can't be empty")

        val player =
            minecraftServer
                .getPlayerByName(playerName)
                ?.takeIf { source.canSee(it) }

        return player ?: throw IllegalArgumentException("Player $playerName not found")
    }

    override fun suggest(
        source: McCommandSource,
        reader: StringReader,
    ): List<String> {
        val playerName = reader.readString()

        return minecraftServer.players
            .map { it.name }
            .filter { it.startsWith(playerName, ignoreCase = true) }
    }
}
