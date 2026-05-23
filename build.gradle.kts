import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.jetbrains.changelog.Changelog

plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    id("org.jetbrains.intellij.platform")
    alias(libs.plugins.changelog)
}

group = "com.example.typespec"
version = "0.2.0-eap.1"

dependencies {
    intellijPlatform {
        intellijIdea(libs.versions.intellij.idea.get())
        bundledPlugin("JavaScript")
        bundledPlugin("NodeJS")
        bundledPlugin("com.intellij.modules.json")
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
        languageVersion = JavaLanguageVersion.of(libs.versions.jdk.get().toInt())
        @Suppress("UnstableApiUsage")
        vendor = JvmVendorSpec.JETBRAINS
    }
}

testing {
    suites {
        @Suppress("UnstableApiUsage")
        named<JvmTestSuite>("test") {
            useJUnitJupiter(libs.versions.junit.get())
        }
    }
}
