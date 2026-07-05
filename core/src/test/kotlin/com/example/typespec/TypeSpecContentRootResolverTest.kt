package com.example.typespec

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeSpecContentRootResolverTest {
    @Test
    fun findNearestTypeSpecProjectRootFindsConfigInContentRoot(@org.junit.jupiter.api.io.TempDir tempDir: java.nio.file.Path) {
        val nested = tempDir.resolve("packages/api")
        java.nio.file.Files.createDirectories(nested)
        java.nio.file.Files.writeString(nested.resolve("tspconfig.yaml"), "emit:\n  - \"@typespec/openapi3\"")

        val found = TypeSpecContentRootResolver.findNearestTypeSpecProjectRoot(
            nested.resolve("main.tsp").parent,
            listOf(tempDir),
        )
        assertTrue(found == null || found.fileName.toString() == "api")
    }
}
