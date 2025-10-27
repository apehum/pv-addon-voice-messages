package dev.apehum.voicemessages

import su.plo.config.Config
import su.plo.config.ConfigField

@Config
data class AddonConfig(
    @ConfigField(comment = "Activation to use for recording")
    val activation: String = "proximity",
    val maxDurationSeconds: Int = 60,
    @ConfigField(comment = "How long recorded voice messages will be stored")
    val expireAfterMinutes: Int = 10,
    val actionbarWhenRecording: Boolean = true,
    @ConfigField(
        comment = """
        Source line weight controls sorting order in "Volume"
        Higher weights are placed at the bottom of the list
    """,
    )
    val sourceLineWeight: Int = 100,
    val chatFormat: ChatFormatConfig = ChatFormatConfig(),
    @ConfigField(
        comment = """
            Available storage types: [MEMORY, REDIS]
        """,
    )
    val storageType: StorageType = StorageType.MEMORY,
    @ConfigField(
        comment = "Redis configuration (required if storageType is REDIS)",
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
