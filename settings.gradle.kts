import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

rootProject.name = "TypeSpecPlugin"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }

    plugins {
        kotlin("jvm") version "2.3.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("org.jetbrains.intellij.platform.settings") version "2.13.1"
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        intellijPlatform {
            defaultRepositories()
        }
    }
}