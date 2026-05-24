package com.example.typespec

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class TypeSpecLspServerLoaderTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun hasLspServerScriptReturnsTrueWhenScriptExists() {
        val packageDirectory = tempDir.resolve("compiler")
        Files.createDirectories(packageDirectory.resolve("cmd"))
        Files.writeString(packageDirectory.resolve("cmd/tsp-server.js"), "// server")

        assertTrue(TypeSpecCompilerPackageResolver.hasLspServerScript(packageDirectory))
    }

    @Test
    fun hasLspServerScriptReturnsFalseWhenScriptMissing() {
        val packageDirectory = tempDir.resolve("compiler")
        Files.createDirectories(packageDirectory)

        assertFalse(TypeSpecCompilerPackageResolver.hasLspServerScript(packageDirectory))
    }
}
