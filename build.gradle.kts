import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.jetbrains.changelog.Changelog

plugins {
    java
    kotlin("jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

group = "com.example.typespec"
version = "0.1.0"

dependencies {
    intellijPlatform {
        intellijIdea("2026.1.1")
        bundledPlugin("JavaScript")
        bundledPlugin("NodeJS")
    }
}

intellijPlatform {
    projectName = "TypeSpecPlugin"

    buildSearchableOptions = false

    pluginConfiguration {
        id = "com.example.typespec"
        name = "TypeSpec Support"
        vendor {
            name = "Example"
        }
        ideaVersion {
            sinceBuild = "261.22158"
        }
        changeNotes = provider {
            changelog.renderItem(
                changelog.getOrNull(project.version.toString()) ?: changelog.getUnreleased(),
                Changelog.OutputType.HTML,
            )
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

testing {
    suites {
        @Suppress("UnstableApiUsage")
        named<JvmTestSuite>("test") {
            useJUnitJupiter()
        }
    }
}
