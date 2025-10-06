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
        val icon: String = "plasmovoice:textures/icons/speaker.png"
        val weight: Int = 100
    }

    companion object {
        private val toml = ConfigurationProvider.getProvider<ConfigurationProvider>(TomlConfiguration::class.java)

        fun loadConfig(server: PlasmoVoiceServer): AddonConfig {
            val addonFolder = File(server.minecraftServer.configsFolder, "pv-addon-voice-messages")
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
