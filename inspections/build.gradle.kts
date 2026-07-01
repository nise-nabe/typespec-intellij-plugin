import org.gradle.api.plugins.jvm.JvmTestSuite
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("typespec.intellij-module-conventions")
    `java-test-fixtures`
}

dependencies {
    implementation(project(":core"))
    intellijPlatform {
        bundledPlugin("intellij.libraries.misc.plugin")
        bundledPlugin("com.intellij.modules.json")
        bundledModule("intellij.json.backend")
        bundledPlugin("JavaScript")
        bundledPlugin("NodeJS")
        testBundledPlugin("intellij.libraries.misc.plugin")
        testBundledPlugin("com.intellij.modules.json")
        testBundledModule("intellij.json.backend")
        testBundledModule("intellij.spellchecker")
        testBundledModule("intellij.platform.smRunner")
        testFramework(TestFrameworkType.Platform)
    }
}

testing {
    suites {
        @Suppress("UnstableApiUsage")
        named<JvmTestSuite>("test") {
            useJUnitJupiter(libs.versions.junit.get())

            dependencies {
                implementation(libs.junit.junit4)
                runtimeOnly(libs.junit.vintage.engine)
            }
        }
    }
}
