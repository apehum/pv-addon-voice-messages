pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.plasmoverse.com/releases")
        maven("https://jitpack.io/")

        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots")

        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev")
        maven("https://maven.neoforged.net/releases/")
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.kikugie.dev/snapshots")
    }

    versionCatalogs {
        create("ft") { from("dev.kikugie.fletching-table:fletching-table.catalog:0.2-SNAPSHOT") }
    }
}

rootProject.name = "pv-addon-voice-messages"

plugins {
    id("dev.kikugie.stonecutter") version "0.10"
}

include("api")
include("api-example")
include("integration:paper")

stonecutter {
    create("client") {
        fun minecraftVersion(
            minecraftVersion: String,
            loaders: List<String>,
        ) {
            for (loader in loaders) {
                version("$minecraftVersion-$loader", minecraftVersion)
            }
        }

        minecraftVersion("1.21.1", loaders = listOf("fabric", "neoforge"))
        minecraftVersion("1.21.11", loaders = listOf("fabric", "neoforge"))
        minecraftVersion("26.3", loaders = listOf("fabric", "neoforge"))

        vcsVersion = "1.21.1-fabric"
    }
}
