import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec

plugins {
    java
    kotlin("jvm")
    id("org.jetbrains.intellij.platform")
}

group = "com.example.typespec"
version = "1.0.0"

dependencies {
    intellijPlatform {
        intellijIdeaUltimate("2025.3.4")
        bundledPlugin("com.intellij.modules.lsp")
        bundledPlugin("JavaScript")
        bundledPlugin("NodeJS")
    }
}

intellijPlatform {
    projectName = "TypeSpecPlugin"

    pluginConfiguration {
        id = "com.example.typespec"
        name = "TypeSpec Support"
        vendor {
            name = "Example"
        }
        ideaVersion {
            sinceBuild = "252.25557"
        }
    }
}

java {
    toolchain {
        // https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html#platformVersions
        languageVersion = JavaLanguageVersion.of(21)
        @Suppress("UnstableApiUsage")
        vendor = JvmVendorSpec.JETBRAINS
    }
}