import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("typespec.kotlin-conventions")
    id("org.jetbrains.intellij.platform.module")
}

dependencies {
    intellijPlatform {
        val ideaVersion = rootProject.extensions.getByType<VersionCatalogsExtension>()
            .named("libs")
            .findVersion("intellij-idea")
            .get()
            .requiredVersion
        intellijIdea(ideaVersion)
    }
}
