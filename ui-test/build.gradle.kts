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
                implementation(libs.remote.robot)
                implementation(libs.junit.junit4)
                runtimeOnly(libs.junit.vintage.engine)
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
