package dev.apehum.voicemessages.util.extension

fun Long.padStartZero(length: Int = 2) = toString().padStart(length, '0')
