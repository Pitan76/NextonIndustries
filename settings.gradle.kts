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
        // cmd /c は workingDir ではなく PATH から探すため、ラッパーは絶対パスで渡す
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

    runSubmoduleGradle(File(rootDir, "core"), "build", "publishToMavenLocal")
    runSubmoduleGradle(File(rootDir, "machinery"), "build")
    runSubmoduleGradle(File(rootDir, "dynamics"), "build")
}
