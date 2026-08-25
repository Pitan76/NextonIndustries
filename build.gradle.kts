import com.matthewprenger.cursegradle.CurseProject
import com.matthewprenger.cursegradle.CurseRelation
import org.gradle.kotlin.dsl.closureOf
import java.security.MessageDigest

plugins {
    id("fabric-loom") version "1.13-SNAPSHOT"
    id("maven-publish")
    id("com.modrinth.minotaur") version "2.+"
    id("com.matthewprenger.cursegradle") version "1.5.0"
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

val targetJavaVersion = 17
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    withSourcesJar()
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("nextonindustries") {
            sourceSet("main")
            sourceSet("client")
        }
    }
}

repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.pitan76.net/")
    maven("https://maven.architectury.dev/")
    flatDir {
        dirs(
            "${project.rootDir}/core/fabric/build/libs",
            "${project.rootDir}/machinery/fabric/build/libs",
            "${project.rootDir}/dynamics/build/libs"
        )
    }
}

fun fileHash(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { fis ->
        val buffer = ByteArray(8192)
        var read = fis.read(buffer)
        while (read != -1) {
            digest.update(buffer, 0, read)
            read = fis.read(buffer)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

val coreJar = files("./core/fabric/build/libs/nextoncore-${project.version}.jar")
val machineryJar = files("./machinery/fabric/build/libs/nextonmachinery-${project.version}.jar")
val dynamicsJar = files("./dynamics/build/libs/nextondynamics-${project.version}.201.jar")

val submoduleHashFile = layout.buildDirectory.file("submodule-hash.txt").get().asFile

val submoduleJars = mapOf(
    "nextoncore" to coreJar,
    "nextonmachinery" to machineryJar,
    "nextondynamics" to dynamicsJar,
)

run {
    val current = submoduleJars.mapValues { (_, jar) ->
        jar.singleFile.takeIf { it.exists() }?.let { fileHash(it) } ?: ""
    }

    val previous = if (submoduleHashFile.exists()) {
        submoduleHashFile.readLines()
            .mapNotNull { line -> line.split("=", limit = 2).takeIf { it.size == 2 } }
            .associate { it[0] to it[1] }
    } else emptyMap()

    val changed = current.filterKeys { current[it] != previous[it] }.keys

    if (changed.isNotEmpty()) {
        logger.lifecycle("changed submodules: ${changed.joinToString(", ")}")

        val remappedDir = file(".gradle/loom-cache/remapped_mods/remapped/net/pitan76")
        val home = System.getProperty("user.home")

        changed.forEach { name ->
            remappedDir.listFiles()
                ?.filter { it.name == name || it.name.startsWith("$name-") }
                ?.forEach {
                    logger.lifecycle("delete remap cache: ${it.name}")
                    it.deleteRecursively()
                }

            val dir = File("$home/.gradle/caches/modules-2/files-2.1/net.pitan76/$name")
            if (dir.exists()) {
                logger.lifecycle("delete module cache: $name")
                dir.deleteRecursively()
            }
        }

        if (!submoduleHashFile.parentFile.exists()) submoduleHashFile.parentFile.mkdirs()
        submoduleHashFile.writeText(
            current.entries.joinToString(System.lineSeparator()) { "${it.key}=${it.value}" }
        )
    }
}

tasks.register<Exec>("buildCore") {
    workingDir = file("./core")

    if (System.getProperty("os.name").lowercase().contains("windows")) {
        commandLine("cmd", "/c", "gradlew.bat", "build")
    } else {
        commandLine("./gradlew", "build")
    }
}

tasks.register<Exec>("buildMachinery") {
    workingDir = file("./machinery")

    if (System.getProperty("os.name").lowercase().contains("windows")) {
        commandLine("cmd", "/c", "gradlew.bat", "build")
    } else {
        commandLine("./gradlew", "build")
    }
}

tasks.register<Exec>("buildDynamics") {
    workingDir = file("./dynamics")

    if (System.getProperty("os.name").lowercase().contains("windows")) {
        commandLine("cmd", "/c", "gradlew.bat", "build")
    } else {
        commandLine("./gradlew", "build")
    }
}

tasks.register("buildSubmodules") {
    group = "nexton"
    dependsOn("buildCore", "buildMachinery", "buildDynamics")
}

fun gradlewCommand(vararg args: String): List<String> =
    if (System.getProperty("os.name").lowercase().contains("windows")) {
        listOf("cmd", "/c", "gradlew.bat") + args
    } else {
        listOf("./gradlew") + args
    }

listOf("runClient", "runServer").forEach { target ->
    tasks.register<Exec>("dev${target.replaceFirstChar { it.uppercase() }}") {
        group = "nexton"
        dependsOn("buildSubmodules")
        workingDir = project.projectDir
        commandLine(gradlewCommand(target))
    }
}

tasks.named("build") {
    dependsOn("buildSubmodules")
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${project.property("fabric_loader_version")}")
    modImplementation("net.pitan76:mcpitanlib-fabric-${project.property("mcpitanlib_version")}")

    modImplementation("net.pitan76:nextoncore:${project.version}")
    modImplementation("net.pitan76:nextonmachinery:${project.version}")
    modImplementation("net.pitan76:nextondynamics:${project.version}.201")

    include("net.pitan76:nextoncore:${project.version}")
    include("net.pitan76:nextonmachinery:${project.version}")
    include("net.pitan76:nextondynamics:${project.version}.201")

    modImplementation("RebornCore:RebornCore-1.20:5.10.4")
    modImplementation("TechReborn:TechReborn-1.20:5.10.4")
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", project.property("minecraft_version"))
    inputs.property("fabric_loader_version", project.property("fabric_loader_version"))
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to project.property("minecraft_version") as String,
            "fabric_loader_version" to project.property("fabric_loader_version") as String,
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.property("archives_base_name") as String
            from(components["java"])
        }
    }

    repositories {

    }
}

if (System.getenv("CURSEFORGE_TOKEN") != null) {
    curseforge {
        apiKey = System.getenv("CURSEFORGE_TOKEN")
        project(closureOf<CurseProject> {
            id = "nextonindustries"
            changelog = project.property("changelog") as String + "\nMCPitanLib version: " + (project.property("mcpitanlib_version") as String).split(":")[1]
            releaseType = "release"

            gameVersionStrings.addAll(listOf("1.18", "1.18.1", "1.18.2", "1.19", "1.19.1", "1.19.2", "1.20.1", "1.20.3", "1.20.4", "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11", "26.1", "26.1.1", "26.1.2", "26.2"))

            addGameVersion("Fabric")
            mainArtifact(tasks.named("remapJar").get().outputs.files.singleFile)

            relations(closureOf<CurseRelation> {
//                requiredDependency("fabric-api")
                requiredDependency("mcpitanlibarch")
            })
        })
    }
}

if (System.getenv("MODRINTH_TOKEN") != null) {
    modrinth {
        token.set(System.getenv("MODRINTH_TOKEN"))
        projectId.set("nextonindustries")
        versionNumber.set(project.property("mod_version") as String + "-fabric")

        gameVersions.addAll(listOf("1.18", "1.18.1", "1.18.2", "1.19", "1.19.1", "1.19.2", "1.20.1", "1.20.3", "1.20.4", "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11", "26.1", "26.1.1", "26.1.2", "26.2"))

        versionType.set("release")
        uploadFile.set(tasks.named("remapJar").get().outputs.files.singleFile)
        changelog.set(project.property("changelog") as String + "\nMCPitanLib version: " + (project.property("mcpitanlib_version") as String).split(":")[1])
        loaders.set(listOf("fabric"))

        dependencies {
//            required.project("P7dR8mSH") // Fabric API
            required.project("uNRoUnGT") // MCPitanLib
        }
    }
}
