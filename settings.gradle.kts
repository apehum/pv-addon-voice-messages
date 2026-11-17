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
        fun target(
            name: String,
            minecraftVersion: String,
            vararg loaders: String,
        ) = loaders.forEach { version("$name-$it", minecraftVersion) }

        target("1.21.1", "1.21.1", "fabric", "neoforge")
        target("1.21.11", "1.21.11", "fabric", "neoforge")
        target("26.1", "26.1.2", "fabric", "neoforge")
        target("26.2", "26.2", "fabric", "neoforge")
        target("26.3", "26.3", "fabric", "neoforge")

        vcsVersion = "1.21.1-fabric"
    }
}
