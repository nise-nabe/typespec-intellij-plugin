plugins {
    id("typespec.kotlin-conventions")
    id("org.jetbrains.intellij.platform.module")
}

val ideaVersion = versionCatalogs.named("libs")
    .findVersion("intellij-idea")
    .orElseThrow {
        GradleException(
            "Version alias 'intellij-idea' is not defined in gradle/libs.versions.toml [versions].",
        )
    }
    .requiredVersion

dependencies {
    intellijPlatform {
        intellijIdea(ideaVersion)
    }
}
