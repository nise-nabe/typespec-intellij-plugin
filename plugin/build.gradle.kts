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
