import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("typespec.kotlin-conventions")
    id("org.jetbrains.intellij.platform")
    alias(libs.plugins.changelog)
}

group = "com.example.typespec"
version = "0.2.0-eap.1"

dependencies {
    intellijPlatform {
        intellijIdea(libs.versions.intellij.idea.get())
        bundledPlugin("JavaScript")
        bundledPlugin("NodeJS")
        bundledPlugin("com.intellij.modules.json")
        testFramework(TestFrameworkType.Platform)
        pluginComposedModule(project(":core"))
        pluginComposedModule(project(":lsp"))
        pluginComposedModule(project(":inspections"))
        pluginComposedModule(project(":actions"))
    }

}

testing {
    suites {
        @Suppress("UnstableApiUsage")
        named<JvmTestSuite>("test") {
            useJUnitJupiter(libs.versions.junit.get())

            dependencies {
                implementation(project(":actions"))
                implementation(project(":core"))
                implementation(project(":lsp"))
                implementation(project(":inspections"))
                implementation("junit:junit:${libs.versions.junit4.get()}")
                runtimeOnly("org.junit.vintage:junit-vintage-engine:${libs.versions.junit.get()}")
            }
        }
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
            sinceBuild = "262.0"
        }
        changeNotes = provider {
            changelog.renderItem(
                changelog.getOrNull(project.version.toString()) ?: changelog.getUnreleased(),
                Changelog.OutputType.HTML,
            )
        }
    }
}

changelog {
    path.set(rootProject.layout.projectDirectory.file("CHANGELOG.md").asFile.absolutePath)
}

intellijPlatformTesting {
    runIde {
        register("runIdeForUiTests") {
            task {
                jvmArgumentProviders.add(
                    CommandLineArgumentProvider {
                        listOf(
                            "-Drobot-server.port=8082",
                            "-Dide.mac.message.dialogs.as.sheets=false",
                            "-Djb.privacy.policy.text=<!--999.999-->",
                            "-Djb.consents.confirmation.enabled=false",
                        )
                    },
                )
            }
            plugins {
                robotServerPlugin()
            }
        }
    }
}
