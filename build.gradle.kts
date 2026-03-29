import org.gradle.api.plugins.jvm.JvmTestSuite
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
        intellijIdea("2026.1")
        bundledPlugin("com.intellij.modules.lsp")
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
