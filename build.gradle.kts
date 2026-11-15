plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.pv.entrypoints)
    alias(libs.plugins.pv.kotlin.relocate)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.runpaper)
    alias(libs.plugins.modstitch) apply false
}

dependencies {
    compileOnly(libs.plasmovoice)

    // access for shaded adventure library
    compileOnly(variantOf(libs.slib) { classifier("all") })

    compileOnly(libs.kotlinx.serialization.core)

    implementation(project(":api")) {
        isTransitive = false
    }

    implementation(libs.oggus) {
        isTransitive = false
    }
    implementation(libs.jedis) {
        exclude("org.slf4j")
        exclude("com.google.code.gson")
    }

    implementation(libs.msgpack) {
        isTransitive = false
    }

    implementation(project(":integration:paper"))

    testImplementation(kotlin("test"))
    testImplementation(kotlin("stdlib-jdk8"))
    testImplementation(project(":api"))
    testImplementation(project(":integration:paper"))

    testImplementation(libs.plasmovoice)
    testImplementation(libs.kotlinx.coroutines)
    testImplementation(libs.kotlinx.coroutines.jdk8)
    testImplementation(libs.kotlinx.serialization.core)
    testImplementation(libs.msgpack)

    testImplementation(libs.oggus)
}

allprojects {
    if (path == ":client" || path.startsWith(":client:")) return@allprojects

    apply(plugin = "kotlin")

    java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

    dependencies {
        compileOnly(kotlin("stdlib-jdk8"))
        compileOnly(rootProject.libs.kotlinx.coroutines)
        compileOnly(rootProject.libs.kotlinx.coroutines.jdk8)
    }

    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://repo.plasmoverse.com/releases")
        maven("https://repo.plasmoverse.com/snapshots")
    }
}

buildConfig {
    packageName = project.group.toString()

    // entrypoint generator doesn't support static kotlin source files parsing
    useJavaOutput()

    buildConfigField("VERSION", project.version.toString())
    buildConfigField("PROJECT_NAME", project.name)
}

shadow {
    // don't publish fat jar
    addShadowVariantIntoJavaComponent = false
}

tasks {
    test {
        useJUnitPlatform()
    }

    jar {
        exclude(
            "fabric.mod.json",
            "plugin.yml",
            "extension.json",
            "pack.mcmeta",
            "META-INF/*.toml",
            "dev/apehum/voicemessages/*EntryPoint.class",
        )
    }

    shadowJar {
        configurations = listOf(project.configurations.runtimeClasspath.get())

        SHADED_LIBRARIES.forEach { (packageName, libraryName) ->
            relocate(packageName, "${project.group}.libraries.$libraryName")
        }

        mergeServiceFiles()
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.get())

        downloadPlugins {
            modrinth("plasmo-voice", "spigot-${libs.versions.plasmovoice.get()}")
            modrinth("packetevents", "2.11.1+spigot")

//            modrinth("carbon", "6gfp1kIe")
            modrinth("LuckPerms", "v5.5.17-bukkit")
            modrinth("placeholderapi", "2.11.7")
            modrinth("commandapi", "jKB5ZyW7")
        }
    }
}
