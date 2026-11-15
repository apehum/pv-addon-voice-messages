package dev.apehum.voicemessages.channel.codec

import com.ensarsarajcic.kotlinx.serialization.msgpack.MsgPack
import com.ensarsarajcic.kotlinx.serialization.msgpack.MsgPackConfiguration

val channelMsgPack = MsgPack(MsgPackConfiguration(ignoreUnknownKeys = true))
