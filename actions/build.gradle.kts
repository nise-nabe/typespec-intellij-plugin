import org.gradle.api.plugins.jvm.JvmTestSuite
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("typespec.intellij-module-conventions")
}

dependencies {
    implementation(project(":core"))
    intellijPlatform {
        bundledPlugin("JavaScript")
        bundledPlugin("NodeJS")
        testFramework(TestFrameworkType.Platform)
    }
}

testing {
    suites {
        @Suppress("UnstableApiUsage")
        named<JvmTestSuite>("test") {
            useJUnitJupiter(libs.versions.junit.get())

            dependencies {
                implementation("junit:junit:${libs.versions.junit4.get()}")
                runtimeOnly("org.junit.vintage:junit-vintage-engine:${libs.versions.junit.get()}")
            }
        }
    }
}
