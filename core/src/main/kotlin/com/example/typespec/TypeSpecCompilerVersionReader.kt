package com.example.typespec

import java.nio.file.Files
import java.nio.file.Path

object TypeSpecCompilerVersionReader {
    private val versionPattern = Regex(""""version"\s*:\s*"([^"]+)"""")

    fun readPackageVersion(packageDirectory: Path): String? {
        val packageJson = packageDirectory.resolve("package.json")
        if (!Files.isRegularFile(packageJson)) {
            return null
        }
        return versionPattern.find(Files.readString(packageJson))?.groupValues?.getOrNull(1)
    }
}
