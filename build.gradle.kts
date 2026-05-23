
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease
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
version = "0.2.0-eap.1"

dependencies {
    intellijPlatform {
        intellijIdea("262.6228.19")
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
            sinceBuild = "262.0"
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
