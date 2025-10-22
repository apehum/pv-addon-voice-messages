package dev.apehum.voicemessages

import su.plo.config.Config
import su.plo.config.ConfigField

@Config
data class AddonConfig(
    val activation: String = "proximity",
    val maxDurationSeconds: Int = 60,
    val actionbarWhenRecording: Boolean = true,
    val sourceLine: SourceLineConfig = SourceLineConfig(),
    val chatFormat: ChatFormatConfig = ChatFormatConfig(),
    @ConfigField(
        comment = """
            Available storage types: [MEMORY, REDIS]
        """,
    )
    val storageType: StorageType = StorageType.MEMORY,
    @ConfigField(
        nullComment = """
            [redis]
            host = "localhost"
            port = 6379
            user = ""
            password = ""
        """,
    )
    val redis: RedisStorageConfig? = null,
) {
    @Config
    data class SourceLineConfig(
        val icon: String = "plasmovoice:textures/icons/speaker.png",
        val weight: Int = 100,
    )

    enum class StorageType {
        MEMORY,
        REDIS,
    }

    @Config
    data class RedisStorageConfig(
        val host: String = "localhost",
        val port: Int = 6379,
        val user: String = "",
        val password: String = "",
    )

    @Config
    data class ChatFormatConfig(
        val default: String = "<lang:chat.type.text:'<player_name>':'<voice_message>'>",
        val directIncoming: String = "<italic><gray><lang:commands.message.display.incoming:'<source_player_name>':'<voice_message>'>",
        val directOutgoing: String = "<italic><gray><lang:commands.message.display.outgoing:'<target_player_name>':'<voice_message>'>",
    )
}
