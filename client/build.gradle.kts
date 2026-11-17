import groovy.json.JsonSlurper
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import java.util.zip.ZipFile

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("dev.isxander.modstitch.base")
    id("com.gradleup.shadow")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.buildconfig)
    alias(ft.plugins.relocate)
}

val mcVersion = stonecutter.current.version
val loader = stonecutter.current.project.substringAfterLast('-')
val isFabric = loader == "fabric"
val plasmoVoiceVersion =
    libs.versions.pv.client
        .get()

base.archivesName = "${rootProject.name}-${rootProject.version}+$mcVersion-$loader"

fun propertyVersion(name: String) = property("versions.$name") as String

val shaded = configurations.create("shaded")

sourceSets {
    main {
        compileClasspath += shaded
    }
}

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

modstitch {
    minecraftVersion = mcVersion

    loom {
        fabricLoaderVersion = propertyVersion("fabric_loader")

        configureLoom {
            sourceSets.main {
                runtimeClasspath += shaded
            }

            mods {
                maybeCreate("pv_addon_voice_messages").apply {
                    sourceSet(sourceSets.main.get())
                    configuration(shaded)
                }
            }
        }
    }

    moddevgradle {
        neoForgeVersion = propertyVersion("neoforge")

        defaultRuns(server = false)
    }

    metadata {
        // neoforge doesn't allow dashes in mod ids
        modId = if (isFabric) rootProject.name else rootProject.name.replace('-', '_')
        modName = rootProject.name
        modVersion = rootProject.version.toString()
        modGroup = rootProject.group.toString()
        modDescription = "Voice messages addon"
        modLicense = "MIT"

        replacementProperties.put("plasmovoice_version", plasmoVoiceVersion)
    }

    mixin {
        addMixinsToModManifest = true

        configs.register("pv-addon-voice-messages") {
            side = CLIENT
        }
    }
}

java {
    toolchain.languageVersion = modstitch.javaVersion.map(JavaLanguageVersion::of)
}

val nestedJarMods = mutableListOf<Any>()

fun DependencyHandler.modLocalRuntimeNested(notation: Any) {
    add("modstitchModLocalRuntime", notation)
    nestedJarMods += notation
}

fun ZipFile.nestedJarEntries(): List<String> {
    val modJson = getEntry("fabric.mod.json") ?: return emptyList()
    val json = getInputStream(modJson).use { JsonSlurper().parse(it) } as Map<*, *>

    return (json["jars"] as? List<*>)
        .orEmpty()
        .mapNotNull { (it as? Map<*, *>)?.get("file") as? String }
}

// loom doesn't put jar-in-jar dependencies of a mod on the dev runtime classpath
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

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.plasmoverse.com/releases")
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(rootProject.libs.kotlinx.coroutines)
    compileOnly(rootProject.libs.kotlinx.coroutines.jdk8)
    compileOnly(rootProject.libs.kotlinx.serialization.core)

    val plasmoVoice = "maven.modrinth:plasmo-voice:$loader-$mcVersion-$plasmoVoiceVersion"

    modstitchModCompileOnly(plasmoVoice)

    shaded(project(":"))

    if (isFabric) {
        modstitchModCompileOnly("net.fabricmc.fabric-api:fabric-api:${propertyVersion("fabric_api")}")

        modLocalRuntimeNested(plasmoVoice)
        modstitchModLocalRuntime("net.fabricmc.fabric-api:fabric-api:${propertyVersion("fabric_api")}")

        modstitchModLocalRuntime(kotlin("stdlib-jdk8"))
        modstitchModLocalRuntime(rootProject.libs.kotlinx.coroutines)
        modstitchModLocalRuntime(rootProject.libs.kotlinx.coroutines.jdk8)
        modstitchModLocalRuntime(rootProject.libs.kotlinx.serialization.core)
        modstitchModLocalRuntime(rootProject.libs.msgpack)

        modstitchModLocalRuntime(files(provider { unpackNestedJars() }))
    } else {
        modstitchModLocalRuntime(plasmoVoice)

        modstitchLocalRuntime(kotlin("stdlib-jdk8"))
        modstitchLocalRuntime(rootProject.libs.kotlinx.coroutines)
        modstitchLocalRuntime(rootProject.libs.kotlinx.coroutines.jdk8)
        modstitchLocalRuntime(rootProject.libs.kotlinx.serialization.core)
        modstitchLocalRuntime(rootProject.libs.msgpack)
    }
}

if (!isFabric) {
    val unpackServerAddon =
        tasks.register<Sync>("unpackServerAddon") {
            from(zipTree(serverAddonJar.flatMap { it.archiveFile }))
            into(layout.buildDirectory.dir("server-addon"))
        }

    val serverAddon =
        sourceSets.create("serverAddon") {
            output.dir(mapOf("builtBy" to unpackServerAddon), unpackServerAddon.map { it.destinationDir })
        }

    sourceSets.main {
        runtimeClasspath += serverAddon.output
    }

    configure<NeoForgeExtension> {
        mods.named("mod") {
            sourceSet(serverAddon)
        }
    }
}

buildConfig {
    packageName = "dev.apehum.voicemessages.client"

    useJavaOutput()

    buildConfigField("VERSION", modstitch.metadata.modVersion)
    buildConfigField("ADDON_ID", rootProject.name)
}

fletchingTable {
    relocate.configure(sourceSets.main) {
        matching("(*).json5") {
            into("$1.json")
            with(Json5ToJson(jsonIndent = "    "))
        }
    }
}

stonecutter {
    replacements.string(current.parsed < "1.21.11") {
        replace("Identifier", "ResourceLocation")
    }

    replacements.string(current.parsed >= "26.3") {
        replace("GuiGraphics", "GuiGraphicsExtractor")
        replace("renderWidget", "extractWidgetRenderState")
    }
}

tasks {
    withType<AbstractArchiveTask>().configureEach {
        archiveVersion = ""
    }

    processResources {
        if (stonecutter.current.parsed >= "1.21.6") {
            exclude("assets/pv_addon_voice_messages/shaders/core/*.json")
        }

        filesMatching(listOf("**/*.vsh", "**/*.fsh")) {
            filter { line -> line.takeUnless { it.trimStart().startsWith("//") } }
        }
    }

    shadowJar {
        configurations = listOf(shaded)

        relocate("kotlin", "su.plo.voice.libs.kotlin")
        relocate("kotlinx.coroutines", "su.plo.voice.libs.kotlinx.coroutines")
        relocate("kotlinx.serialization", "su.plo.voice.libs.kotlinx.serialization")

        SHADED_LIBRARIES.forEach { (packageName, libraryName) ->
            relocate(packageName, "${rootProject.group}.libraries.$libraryName")
        }

        mergeServiceFiles()
    }

    if (modstitch.platform.isLoomRemap) {
        named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
            dependsOn(shadowJar)
            inputFile.set(shadowJar.flatMap { it.archiveFile })
        }
    } else {
        jar {
            archiveClassifier = "dev"
        }

        shadowJar {
            archiveClassifier = ""
        }
    }

    val finalJar = if (modstitch.platform.isLoomRemap) modstitch.finalJarTask else shadowJar

    val copyJarToRootProject =
        register("copyJarToRootProject") {
            val source = finalJar.flatMap { it.archiveFile }
            val target =
                rootProject.layout.buildDirectory
                    .dir("libs")
                    .zip(finalJar.flatMap { it.archiveFileName }) { dir, name -> dir.file(name) }

            inputs.file(source)
            outputs.file(target)

            doLast {
                source.get().asFile.copyTo(target.get().asFile, overwrite = true)
            }
        }

    build {
        dependsOn(copyJarToRootProject)
    }
}
