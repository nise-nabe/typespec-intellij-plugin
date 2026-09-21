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
                implementation(libs.okhttp)
                implementation(libs.junit.junit4)
                runtimeOnly(libs.junit.vintage.engine)
            }

            targets {
                all {
                    testTask.configure {
                        // The outcome depends on a running sandbox IDE, so cached/UP-TO-DATE
                        // results are meaningless — always execute.
                        outputs.upToDateWhen { false }
                        // remote-robot's Retrofit/Gson client reflects into JDK internals on modern JDKs
                        // (JetBrains/intellij-ui-test-robot#355); without these callJs fails with
                        // "Unable to create converter for class ...RetrieveResponse".
                        jvmArgs(
                            "--add-opens=java.base/java.lang=ALL-UNNAMED",
                            "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
                            "--add-opens=java.base/java.util=ALL-UNNAMED",
                            "--add-opens=java.base/java.io=ALL-UNNAMED",
                            "--add-opens=java.base/java.nio=ALL-UNNAMED",
                            "--add-opens=java.base/java.text=ALL-UNNAMED",
                            "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                        )
                        systemProperty(
                            "robot.server.url",
                            providers.systemProperty("robot.server.url")
                                .orElse(providers.gradleProperty("robot.server.url"))
                                .orElse("http://127.0.0.1:8082")
                                .get(),
                        )
                    }
                }
            }
        }
    }
}
