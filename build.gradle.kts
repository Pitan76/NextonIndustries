import com.matthewprenger.cursegradle.CurseProject
import com.matthewprenger.cursegradle.CurseRelation
import org.gradle.kotlin.dsl.closureOf

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
    maven(url = "https://maven.pitan76.net/")
}

val machineryJar = layout.projectDirectory.file("../machinery/fabric/build/libs/machinery-1.0.0.jar").asFile
val dynamicsJar = layout.projectDirectory.file("../dynamics/build/libs/nextondynamics-1.0.0.201.jar").asFile

tasks.register<Exec>("buildMachinery") {
    workingDir = file("../machinery")

    if (System.getProperty("os.name").lowercase().contains("windows")) {
        commandLine("cmd", "/c", "gradlew.bat", "build")
    } else {
        commandLine("./gradlew", "build")
    }
}

tasks.register<Exec>("buildDynamics") {
    workingDir = file("../dynamics")

    if (System.getProperty("os.name").lowercase().contains("windows")) {
        commandLine("cmd", "/c", "gradlew.bat", "build")
    } else {
        commandLine("./gradlew", "build")
    }
}

tasks.named("build") {
    dependsOn("buildMachinery")
    dependsOn("buildDynamics")
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${project.property("fabric_loader_version")}")
    modImplementation("net.pitan76:mcpitanlib-fabric-${project.property("mcpitanlib_version")}") {
        exclude(group = "net.fabricmc.fabric-api")
        exclude(group = "dev.architectury")
    }

    if (machineryJar.exists()) {
        modImplementation(files(machineryJar))
        include(files(machineryJar))
    }

    if (dynamicsJar.exists()) {
        modImplementation(files(dynamicsJar))
        include(files(dynamicsJar))
    }

//    modImplementation(files("dynamics/build/libs/machinery.jar"))

//    include(project(":dynamics"))
//    include(project(":machinery:common"))
//    include(project(":machinery:fabric"))
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

            gameVersionStrings.addAll(listOf("1.18", "1.18.1", "1.18.2", "1.19", "1.19.1", "1.19.2", "1.20.1", "1.20.3", "1.20.4", "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11", "26.1", "26.1.1"))

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

        gameVersions.addAll(listOf("1.18", "1.18.1", "1.18.2", "1.19", "1.19.1", "1.19.2", "1.20.1", "1.20.3", "1.20.4", "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11"))

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
