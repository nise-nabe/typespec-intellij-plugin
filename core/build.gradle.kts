import org.gradle.api.plugins.jvm.JvmTestSuite
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("typespec.intellij-module-conventions")
    `java-test-fixtures`
}

dependencies {
    intellijPlatform {
        bundledPlugin("JavaScript")
        bundledPlugin("NodeJS")
        testFramework(TestFrameworkType.Platform)
    }
    testFixturesImplementation(libs.junit.junit4)
}

testing {
    suites {
        @Suppress("UnstableApiUsage")
        named<JvmTestSuite>("test") {
            useJUnitJupiter(libs.versions.junit.get())
        }
    }
}
