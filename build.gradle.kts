plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    alias(libs.plugins.pv.entrypoints)
    alias(libs.plugins.pv.kotlin.relocate)
    alias(libs.plugins.pv.java.templates)

    id("xyz.jpenilla.run-paper") version "2.3.1"
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    compileOnly(libs.plasmovoice)
    compileOnly(libs.kotlinx.coroutines)
    compileOnly(libs.kotlinx.coroutines.jdk8)

    // access for shaded adventure library
    compileOnly("su.plo.slib:common:1.1.4:all")

    implementation("org.chenliang.oggus:oggus:1.2.0")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("stdlib-jdk8"))

    testImplementation(libs.plasmovoice)
    testImplementation(libs.kotlinx.coroutines)
    testImplementation(libs.kotlinx.coroutines.jdk8)
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.plasmoverse.com/snapshots")
    maven("https://repo.plasmoverse.com/releases")
}

tasks {
    jar {
        enabled = false
    }

    test {
        useJUnitPlatform()
    }

    shadowJar {
        archiveClassifier.set("")
    }

    runServer {
        minecraftVersion("1.21.10")

        downloadPlugins {
            modrinth("plasmo-voice", "spigot-2.1.6")
        }
    }
}
