import groovy.json.JsonSlurper
import net.fabricmc.loom.util.ModPlatform
import java.util.zip.ZipFile

plugins {
    id("dev.architectury.loom")
    id("com.gradleup.shadow")
    alias(libs.plugins.buildconfig)
}

val minecraftVersion = stonecutter.current.version
val modLoader = loom.platform.get()
val isFabric = modLoader == ModPlatform.FABRIC

version = "${rootProject.version}+$minecraftVersion+${modLoader.name.lowercase()}"
base.archivesName = "${rootProject.name}-client"

fun propertyVersion(name: String) = property("versions.$name") as String

val shaded: Configuration by configurations.creating

sourceSets {
    main {
        compileClasspath += shaded
        runtimeClasspath += shaded
    }
}

repositories {
    maven("https://maven.neoforged.net/releases/")

    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven") {
                name = "Modrinth"
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

// todo: modrinth once 2.1.14 is out
// val plasmoVoice = "maven.modrinth:plasmo-voice:${modLoader.name.lowercase()}-$minecraftVersion-${libs.versions.pv.client.get()}"
val plasmoVoice = fileTree("run/mods") { include("plasmovoice-*.jar") }

val serverAddonJar =
    tasks.register<Jar>("serverAddonJar") {
        val addonJar = rootProject.tasks.named<Jar>("shadowJar")

        dependsOn(addonJar)
        archiveClassifier = "server-addon"

        from(zipTree(addonJar.flatMap { it.archiveFile })) {
            exclude(
                "fabric.mod.json",
                "plugin.yml",
                "extension.json",
                "META-INF/*.toml",
                "dev/apehum/voicemessages/*EntryPoint.class",
            )
        }
    }

val nestedJarMods = mutableListOf<String>()

fun DependencyHandler.modLocalRuntimeNested(notation: String) {
    modLocalRuntime(notation)
    nestedJarMods += notation
}

fun ZipFile.nestedJarEntries(): List<String> {
    val modJson = getEntry("fabric.mod.json") ?: return emptyList()
    val json = getInputStream(modJson).use { JsonSlurper().parse(it) } as Map<*, *>

    return (json["jars"] as? List<*>)
        .orEmpty()
        .mapNotNull { (it as? Map<*, *>)?.get("file") as? String }
}

fun unpackNestedJars(): List<File> {
    val jars =
        nestedJarMods.map {
            configurations
                .detachedConfiguration(dependencies.create(it))
                .apply { isTransitive = false }
                .singleFile
        }

    val outputDir =
        layout.buildDirectory
            .dir("nested-jars")
            .get()
            .asFile
    val stamp = outputDir.resolve("stamp.txt")
    val fingerprint = jars.joinToString("\n") { "${it.absolutePath}:${it.length()}:${it.lastModified()}" }

    if (stamp.isFile && stamp.readText() == fingerprint) {
        return outputDir.listFiles { file: File -> file.extension == "jar" }.orEmpty().sorted()
    }

    outputDir.deleteRecursively()
    outputDir.mkdirs()

    val unpacked = mutableListOf<File>()
    val queue = ArrayDeque(jars)

    while (queue.isNotEmpty()) {
        ZipFile(queue.removeFirst()).use { zip ->
            for (path in zip.nestedJarEntries()) {
                val entry = zip.getEntry(path) ?: continue
                val target = outputDir.resolve(path.substringAfterLast('/'))
                if (target.exists()) continue

                zip.getInputStream(entry).use { input -> target.outputStream().use { input.copyTo(it) } }
                unpacked += target
                queue += target
            }
        }
    }

    stamp.writeText(fingerprint)
    return unpacked
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())

    check(!plasmoVoice.isEmpty) { "no plasmovoice-*.jar in ${file("run/mods")}" }

    modCompileOnly(plasmoVoice)

    shaded(files(serverAddonJar))

    if (isFabric) {
        modImplementation("net.fabricmc:fabric-loader:${propertyVersion("fabric_loader")}")
        modLocalRuntime("net.fabricmc.fabric-api:fabric-api:${propertyVersion("fabric_api")}")

        modLocalRuntime(kotlin("stdlib-jdk8"))
        modLocalRuntime(rootProject.libs.kotlinx.coroutines)
        modLocalRuntime(rootProject.libs.kotlinx.coroutines.jdk8)
    } else {
        "neoForge"("net.neoforged:neoforge:${propertyVersion("neoforge")}")

        "forgeRuntimeLibrary"(kotlin("stdlib-jdk8"))
        "forgeRuntimeLibrary"(rootProject.libs.kotlinx.coroutines)
        "forgeRuntimeLibrary"(rootProject.libs.kotlinx.coroutines.jdk8)
    }

    modLocalRuntime(files(unpackNestedJars()))
}

buildConfig {
    packageName = "dev.apehum.voicemessages.client"

    useJavaOutput()

    buildConfigField("VERSION", project.version.toString())
    buildConfigField("ADDON_ID", rootProject.name)
}

loom {
    mods {
        val name = if (isFabric) "pv_addon_voice_messages" else "main"

        maybeCreate(name).apply {
            sourceSet(sourceSets.main.get())

            configuration(shaded)
        }
    }
}

stonecutter {
    replacements.string(current.parsed < "1.21.11") {
        replace("Identifier", "ResourceLocation")
    }
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

tasks {
    processResources {
        val manifest = if (isFabric) "fabric.mod.json" else "META-INF/neoforge.mods.toml"

        exclude(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml") - manifest)
        filesMatching(manifest) {
            expand(
                "version" to version,
                "minecraftVersion" to minecraftVersion,
                "plasmoVoiceVersion" to
                    libs.versions.plasmovoice
                        .get(),
            )
        }
    }

    shadowJar {
        configurations = listOf(shaded)

        relocate("kotlin", "su.plo.voice.libs.kotlin")
        relocate("kotlinx.coroutines", "su.plo.voice.libs.kotlinx.coroutines")
    }

    remapJar {
        dependsOn(shadowJar)
        inputFile.set(shadowJar.flatMap { it.archiveFile })
    }
}
