import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.runpaper)
    id("de.eldoria.plugin-yml.bukkit") version "0.8.0"
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(project(":api"))
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")

    compileOnly("de.hexaoxi:carbonchat-api:3.0.0-beta.36")
}

tasks {
    runServer {
        minecraftVersion("1.21.10")

        pluginJars.from(project(":").tasks.shadowJar)

        downloadPlugins {
            modrinth("plasmo-voice", "spigot-2.1.6")
            modrinth("carbon", "6gfp1kIe")
            modrinth("LuckPerms", "v5.5.17-bukkit")
        }
    }
}

bukkit {
    main = "dev.apehum.vmintegration.VoiceMessagesIntegration"
    apiVersion = "1.21"

    depend = listOf("CarbonChat", "pv-addon-voice-messages")
}
