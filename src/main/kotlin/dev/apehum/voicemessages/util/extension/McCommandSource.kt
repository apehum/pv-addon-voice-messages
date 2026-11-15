package dev.apehum.voicemessages.util.extension

import su.plo.slib.api.command.McCommandSource

fun McCommandSource.canRecordWith(chatSenderName: String): Boolean =
    hasPermission("pv.addon.voice_messages.record.*") ||
        hasPermission("pv.addon.voice_messages.record.$chatSenderName")
