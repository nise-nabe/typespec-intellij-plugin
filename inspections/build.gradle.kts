import org.gradle.api.plugins.jvm.JvmTestSuite
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("typespec.intellij-module-conventions")
    `java-test-fixtures`
}

dependencies {
    implementation(project(":core"))
    intellijPlatform {
        bundledPlugin("com.intellij.modules.json")
        testBundledPlugin("intellij.libraries.misc.plugin")
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
