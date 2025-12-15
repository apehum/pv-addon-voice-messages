plugins {
    alias(libs.plugins.kotlin.jvm)
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

    shadow(project(":api")) {
        isTransitive = false
    }

    shadow(libs.oggus) {
        isTransitive = false
    }
    shadow(libs.jedis) {
        exclude("org.slf4j")
        exclude("com.google.code.gson")
    }

    testImplementation(kotlin("test"))
    testImplementation(kotlin("stdlib-jdk8"))
    testImplementation(project(":api"))

    testImplementation(libs.plasmovoice)
    testImplementation(libs.kotlinx.coroutines)
    testImplementation(libs.kotlinx.coroutines.jdk8)

    testImplementation(libs.oggus)
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://repo.plasmoverse.com/releases")
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

    shadowJar {
        configurations = listOf(project.configurations.shadow.get())

        listOf(
            "redis.clients" to "redis",
            "org.chenliang.oggus" to "oggus",
            "org.apache" to "apache",
            "org.json" to "json",
        ).forEach { (packageName, libraryName) ->
            relocate(packageName, "${project.group}.libraries.$libraryName")
        }

        mergeServiceFiles()
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.get())

        downloadPlugins {
            modrinth("plasmo-voice", "spigot-${libs.versions.plasmovoice.get()}")
        }
    }
}
