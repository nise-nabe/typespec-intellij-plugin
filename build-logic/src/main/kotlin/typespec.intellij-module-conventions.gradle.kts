plugins {
    id("typespec.kotlin-conventions")
    id("org.jetbrains.intellij.platform.module")
}

dependencies {
    intellijPlatform {
        intellijIdea(
            providers.gradleProperty("typespec.intellij.idea").orElse("262.6228.19"),
        )
    }
}
