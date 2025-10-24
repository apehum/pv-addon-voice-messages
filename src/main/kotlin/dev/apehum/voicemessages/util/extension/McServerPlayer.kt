package dev.apehum.voicemessages.util.extension

import su.plo.slib.api.server.entity.player.McServerPlayer
import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.api.server.player.VoiceServerPlayer
import kotlin.jvm.optionals.getOrNull

fun McServerPlayer.asVoicePlayer(voiceServer: PlasmoVoiceServer): VoiceServerPlayer =
    voiceServer.playerManager.getPlayerById(uuid).getOrNull() ?: throw IllegalArgumentException("Player not found")
