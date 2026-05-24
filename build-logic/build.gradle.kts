plugins {
    `kotlin-dsl`
}

group = "com.example.typespec.buildlogic"

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.intellij.platform.gradle.plugin)
}
