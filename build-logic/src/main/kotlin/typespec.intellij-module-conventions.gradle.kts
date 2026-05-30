import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("typespec.kotlin-conventions")
    id("org.jetbrains.intellij.platform.module")
}

dependencies {
    intellijPlatform {
        val ideaVersion = project.extensions.getByType<VersionCatalogsExtension>()
            .named("libs")
            .findVersion("intellij-idea")
            .orElseThrow {
                GradleException(
                    "Version alias 'intellij-idea' is not defined in gradle/libs.versions.toml [versions].",
                )
            }
            .requiredVersion
        intellijIdea(ideaVersion)
    }
}
