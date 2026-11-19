import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar

plugins {
    alias(libs.plugins.runpaper)
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://nexus.scarsz.me/content/groups/public/")
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":integration:paper"))
    compileOnly(libs.paper.api)
    compileOnly(libs.discordsrv)
    compileOnly(libs.oggus)
}

runPaper {
    disablePluginJarDetection()
}

tasks {
    runServer {
        minecraftVersion(libs.versions.minecraft.get())

        javaLauncher =
            project.javaToolchains.launcherFor {
                languageVersion = JavaLanguageVersion.of(25)
            }

        pluginJars.from(project(":").tasks.shadowJar)

        downloadPlugins {
            modrinth("plasmo-voice", "paper-${libs.versions.plasmovoice.get()}")
            modrinth("packetevents", "${libs.versions.packetevents.get()}+spigot")
            modrinth("discordsrv", libs.versions.discordsrv.get())
        }
    }
}
