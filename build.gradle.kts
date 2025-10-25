import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.pv.entrypoints)
    alias(libs.plugins.pv.kotlin.relocate)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.runpaper)
    alias(libs.plugins.publish)
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))

    compileOnly(libs.plasmovoice)
    compileOnly(libs.kotlinx.coroutines)
    compileOnly(libs.kotlinx.coroutines.jdk8)

    // access for shaded adventure library
    compileOnly(variantOf(libs.slib) { classifier("all") })

    shadow(libs.oggus) {
        isTransitive = false
    }
    shadow(libs.jedis) {
        exclude("org.slf4j")
        exclude("com.google.code.gson")
    }
    shadow(libs.config)

    testImplementation(kotlin("test"))
    testImplementation(kotlin("stdlib-jdk8"))

    testImplementation(libs.plasmovoice)
    testImplementation(libs.kotlinx.coroutines)
    testImplementation(libs.kotlinx.coroutines.jdk8)

    testImplementation(libs.oggus)
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.plasmoverse.com/releases")
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
            "su.plo.config" to "config",
        ).forEach { (packageName, libraryName) ->
            relocate(packageName, "${project.group}.libraries.$libraryName")
        }

        mergeServiceFiles()
    }

    runServer {
        minecraftVersion("1.21.10")

        downloadPlugins {
            modrinth("plasmo-voice", "spigot-2.1.6")
        }
    }
}

mavenPublishing {
    coordinates(
        groupId = project.group.toString(),
        artifactId = "voice-messages",
    )

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    pom {
        name.set("pv-addon-voice-messages")
        description.set("Plasmo Voice voice messages addon API.")
        inceptionYear.set("2025")
        url.set("https://github.com/Apehum/pv-addon-voice-messages")

        licenses {
            license {
                name.set("GNU Lesser General Public License version 3")
                url.set("https://opensource.org/license/lgpl-3-0")
                distribution.set("https://opensource.org/license/lgpl-3-0")
            }
        }

        developers {
            developer {
                id.set("apehum")
                name.set("Apehum")
                url.set("https://github.com/Apehum")
            }
        }

        scm {
            url.set("https://github.com/Apehum/pv-addon-voice-messages/")
            connection.set("scm:git:git://github.com/Apehum/pv-addon-voice-messages.git")
            developerConnection.set("scm:git:ssh://git@github.com:Apehum/pv-addon-voice-messages.git")
        }
    }

    val hasSigningKey =
        project.hasProperty("signing.keyId") ||
            project.hasProperty("signingInMemoryKey") ||
            System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey") != null

    if (hasSigningKey) {
        signAllPublications()
    } else {
        logger.warn("Signing credentials not found. Publications will not be signed.")
        logger.warn("Configure signing properties (signing.keyId, signing.password, signing.secretKeyRingFile)")
        logger.warn("or signingInMemoryKey/signingInMemoryKeyPassword to enable signing.")
    }
}
