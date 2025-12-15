plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.publish)
}

dependencies {
    api(libs.plasmovoice)
    compileOnly(kotlin("stdlib-jdk8"))
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

mavenPublishing {
    coordinates(
        groupId = project.group.toString(),
        artifactId = "api",
    )

    publishToMavenCentral(true, false)

    pom {
        name.set("pv-addon-voice-messages")
        description.set("Plasmo Voice voice messages addon API.")
        inceptionYear.set("2025")
        url.set("https://github.com/apehum/pv-addon-voice-messages")

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
            url.set("https://github.com/apehum/pv-addon-voice-messages/")
            connection.set("scm:git:git://github.com/apehum/pv-addon-voice-messages.git")
            developerConnection.set("scm:git:ssh://git@github.com:apehum/pv-addon-voice-messages.git")
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
