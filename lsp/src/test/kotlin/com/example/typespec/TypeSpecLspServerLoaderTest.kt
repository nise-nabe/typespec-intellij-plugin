package com.example.typespec

import com.intellij.javascript.nodejs.util.NodePackage
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
    fun isPackageWithServerScriptReturnsTrueWhenScriptExists() {
        val packageDirectory = tempDir.resolve("compiler")
        Files.createDirectories(packageDirectory.resolve("cmd"))
        Files.writeString(packageDirectory.resolve("cmd/tsp-server.js"), "// server")
        val nodePackage = NodePackage(packageDirectory.toString())

        assertTrue(TypeSpecLspServerLoader.isPackageWithServerScript(nodePackage))
    }

    @Test
    fun isPackageWithServerScriptReturnsFalseWhenScriptMissing() {
        val packageDirectory = tempDir.resolve("compiler")
        Files.createDirectories(packageDirectory)
        val nodePackage = NodePackage(packageDirectory.toString())

        assertFalse(TypeSpecLspServerLoader.isPackageWithServerScript(nodePackage))
    }
}
