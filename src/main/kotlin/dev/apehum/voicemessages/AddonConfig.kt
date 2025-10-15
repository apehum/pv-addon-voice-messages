package dev.apehum.voicemessages

import su.plo.config.Config
import su.plo.config.ConfigField
import su.plo.config.provider.ConfigurationProvider
import su.plo.config.provider.toml.TomlConfiguration
import su.plo.voice.api.server.PlasmoVoiceServer
import java.io.File
import java.io.InputStream

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

    companion object {
        private val toml = ConfigurationProvider.getProvider<ConfigurationProvider>(TomlConfiguration::class.java)

        fun loadConfig(server: PlasmoVoiceServer): AddonConfig {
            val addonFolder = File(server.minecraftServer.configsFolder, BuildConfig.PROJECT_NAME)
            val configFile = File(addonFolder, "config.toml")

            server.languages.register(
                ::getLanguageResource,
                File(addonFolder, "languages"),
            )

            return toml
                .load<AddonConfig>(AddonConfig::class.java, configFile, false)
                .also { toml.save(AddonConfig::class.java, it, configFile) }
        }

        private fun getLanguageResource(resourcePath: String): InputStream? =
            AddonConfig::class.java.classLoader.getResourceAsStream(String.format("voice_messages/%s", resourcePath))
    }
}
