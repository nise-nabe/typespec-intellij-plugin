import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

rootProject.name = "TypeSpecIntellijPlugin"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    includeBuild("build-logic")
}

include("core")
include("lsp")
include("actions")
include("inspections")
include("plugin")
include("ui-test")

plugins {
    // Keep in sync with gradle/libs.versions.toml
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("org.jetbrains.intellij.platform.settings") version "2.16.0"
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    rulesMode.set(RulesMode.FAIL_ON_PROJECT_RULES)

    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        intellijPlatform {
            defaultRepositories()
        }
    }
}
