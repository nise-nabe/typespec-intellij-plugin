import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    id("typespec.intellij-module-conventions")
}

dependencies {
    intellijPlatform {
        bundledPlugin("JavaScript")
        bundledPlugin("NodeJS")
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
