package dev.apehum.voicemessages

import su.plo.config.provider.ConfigurationProvider
import su.plo.config.provider.toml.TomlConfiguration
import java.io.File

val toml: ConfigurationProvider = ConfigurationProvider.getProvider(TomlConfiguration::class.java)

inline fun <reified T : Any> loadConfig(
    configFolder: File,
    configName: String = "config",
): T {
    val configFile = File(configFolder, "$configName.toml")

    return toml
        .load<T>(T::class.java, configFile, false)
        .also { toml.save(it, configFile) }
}
