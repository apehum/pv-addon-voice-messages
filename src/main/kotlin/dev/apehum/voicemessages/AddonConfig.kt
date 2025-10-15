package dev.apehum.voicemessages

import su.plo.config.Config
import su.plo.config.ConfigField

@Config
class AddonConfig {
    @ConfigField
    val activation: String = "proximity"

    @ConfigField
    val maxDurationSeconds: Int = 60

    @ConfigField
    val actionbarWhenRecording: Boolean = true

    @ConfigField
    val sourceLine: SourceLineConfig = SourceLineConfig()

    @Config
    class SourceLineConfig {
        @ConfigField
        val icon: String = "plasmovoice:textures/icons/speaker.png"

        @ConfigField
        val weight: Int = 100
    }

    @ConfigField(
        comment = """
            Available storage types: [MEMORY, REDIS]
        """,
    )
    val storageType: StorageType = StorageType.MEMORY

    @ConfigField(
        nullComment = """
            [redis]
            host = "localhost"
            port = 6379
            user = ""
            password = ""
        """,
    )
    val redis: RedisStorageConfig? = null

    enum class StorageType {
        MEMORY,
        REDIS,
    }

    @Config
    class RedisStorageConfig {
        @ConfigField
        val host: String = "localhost"

        @ConfigField
        val port: Int = 6379

        @ConfigField
        val user: String = ""

        @ConfigField
        val password: String = ""
    }
}
