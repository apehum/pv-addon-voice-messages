package dev.apehum.voicemessages.util.extension

import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.entity.player.McPlayer
import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.api.server.language.ServerLanguages

fun McPlayer.translateClientPV(
    voiceServer: PlasmoVoiceServer,
    key: String,
) = McTextComponent.literal(voiceServer.languages.translateClient(this, key))

fun ServerLanguages.translateClient(
    player: McPlayer,
    key: String,
): String = getClientLanguage(player.language)[key] ?: key
