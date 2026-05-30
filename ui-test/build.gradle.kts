import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    id("typespec.kotlin-conventions")
}

testing {
    suites {
        @Suppress("UnstableApiUsage")
        named<JvmTestSuite>("test") {
            useJUnitJupiter(libs.versions.junit.get())

            dependencies {
                implementation("com.intellij.remoterobot:remote-robot:${libs.versions.remote.robot.get()}")
                implementation("junit:junit:${libs.versions.junit4.get()}")
                runtimeOnly("org.junit.vintage:junit-vintage-engine:${libs.versions.junit.get()}")
            }

            targets {
                all {
                    testTask.configure {
                        systemProperty(
                            "robot.server.url",
                            providers.gradleProperty("robot.server.url").orElse("http://127.0.0.1:8082"),
                        )
                    }
                }
            }
        }
    }
}
