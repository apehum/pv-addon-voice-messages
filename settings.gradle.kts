pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.plasmoverse.com/releases")
        maven("https://jitpack.io/")
    }
}

rootProject.name = "pv-addon-voice-messages"

include("api")
include("api-example")
