import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

rootProject.name = "TypeSpecIntellijPlugin"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }

    plugins {
        kotlin("jvm") version "2.3.21"
        id("org.jetbrains.changelog") version "2.5.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("org.jetbrains.intellij.platform.settings") version "2.16.0"
}

dependencyResolutionManagement {
    rulesMode = RulesMode.FAIL_ON_PROJECT_RULES

    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        intellijPlatform {
            defaultRepositories()
        }
    }
}