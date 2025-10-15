plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    alias(libs.plugins.pv.entrypoints)
    alias(libs.plugins.pv.kotlin.relocate)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.runpaper)
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    compileOnly(libs.plasmovoice)
    compileOnly(libs.kotlinx.coroutines)
    compileOnly(libs.kotlinx.coroutines.jdk8)

    // access for shaded adventure library
    compileOnly(variantOf(libs.slib) { classifier("all") })

    implementation(libs.oggus) {
        isTransitive = false
    }
    implementation(libs.jedis) {
        exclude("org.slf4j")
        exclude("com.google.code.gson")
    }

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

buildConfig {
    packageName = project.group.toString()

    // entrypoint generator doesn't support static kotlin source files parsing
    useJavaOutput()

    buildConfigField("VERSION", project.version.toString())
    buildConfigField("PROJECT_NAME", project.name)
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

        listOf(
            "redis.clients" to "redis",
            "org.chenliang.oggus" to "oggus",
            "org.apache" to "apache",
            "org.json" to "json",
        ).forEach { (packageName, libraryName) ->
            relocate(packageName, "${project.group}.libraries.$libraryName")
        }
    }

    runServer {
        minecraftVersion("1.21.10")

        downloadPlugins {
            modrinth("plasmo-voice", "spigot-2.1.6")
        }
    }
}
