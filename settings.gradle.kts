pluginManagement {
    repositories {
        maven("https://maven.architectury.dev/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.pitan76.net/")
        gradlePluginPortal()
    }

    include("industries", "dynamics", "machinery:common", "machinery:fabric")
}

project(":machinery:common").projectDir = file("machinery/common")
project(":machinery:fabric").projectDir = file("machinery/fabric")