plugins {
    id("typespec.intellij-module-conventions")
}

dependencies {
    intellijPlatform {
        bundledPlugin("JavaScript")
        bundledPlugin("NodeJS")
    }
}
