pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.pitan76.net/")
        maven("https://maven.architectury.dev/")
        gradlePluginPortal()
    }
}

val submoduleTriggers = setOf("runClient", "runServer", "build", "assemble")
val requestedTasks = startParameter.taskNames.map { it.substringAfterLast(':') }

if (requestedTasks.any { it in submoduleTriggers } && !startParameter.projectProperties.containsKey("skipSubmodules")) {
    val isWindows = System.getProperty("os.name").lowercase().contains("windows")

    fun runSubmoduleGradle(dir: File, vararg args: String) {
        val wrapper = File(dir, if (isWindows) "gradlew.bat" else "gradlew").absolutePath
        val command = if (isWindows) listOf("cmd", "/c", wrapper) + args else listOf(wrapper) + args

        println("> Building submodule '${dir.name}' (${args.joinToString(" ")})")

        val process = ProcessBuilder(command)
            .directory(dir)
            .redirectErrorStream(true)
            .start()

        process.inputStream.bufferedReader().forEachLine { println("  [${dir.name}] $it") }

        val exit = process.waitFor()
        if (exit != 0) throw GradleException("Failed to build submodule '${dir.name}' (exit code $exit)")
    }

    fun clearRemappedCore(dir: File) {
        val remappedRoot = File(dir, ".gradle/loom-cache/remapped_mods")
        if (!remappedRoot.isDirectory) return

        remappedRoot.walkTopDown()
            .filter { it.isDirectory && it.name.startsWith("nextoncore") }
            .toList()
            .forEach {
                println("> Clearing stale core cache in '${dir.name}': ${it.name}")
                it.deleteRecursively()
            }
    }

    fun coreHash(): String {
        val props = java.util.Properties()
        File(rootDir, "core/gradle.properties").inputStream().use { props.load(it) }
        val version = props.getProperty("mod_version")

        val digest = java.security.MessageDigest.getInstance("SHA-256")
        listOf("common", "fabric").forEach { platform ->
            val jar = File(rootDir, "core/$platform/build/libs/nextoncore-$version.jar")
            if (jar.exists()) digest.update(jar.readBytes())
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    runSubmoduleGradle(File(rootDir, "core"), "assemble", "publishToMavenLocal")

    val hashFile = File(rootDir, ".gradle/core-hash.txt")
    val currentHash = coreHash()

    if (!hashFile.exists() || hashFile.readText() != currentHash) {
        clearRemappedCore(File(rootDir, "machinery"))
        clearRemappedCore(File(rootDir, "dynamics"))

        hashFile.parentFile.mkdirs()
        hashFile.writeText(currentHash)
    }

    runSubmoduleGradle(File(rootDir, "machinery"), "assemble")
    runSubmoduleGradle(File(rootDir, "dynamics"), "assemble")
}
