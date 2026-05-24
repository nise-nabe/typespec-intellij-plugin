import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec

plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    toolchain {
        // Keep in sync with gradle/libs.versions.toml [versions] jdk
        languageVersion = JavaLanguageVersion.of(25)
        @Suppress("UnstableApiUsage")
        vendor = JvmVendorSpec.JETBRAINS
    }
}
