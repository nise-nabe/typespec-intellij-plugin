package com.example.typespec

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class TypeSpecContentRootResolverTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun findsContentRootContainingTspConfig() {
        val nested = tempDir.resolve("packages/api")
        Files.createDirectories(nested)
        Files.writeString(nested.resolve("tspconfig.yaml"), "emit: []")

        val result = TypeSpecContentRootResolver.findNearestTypeSpecProjectRoot(
            nested.resolve("src"),
            listOf(tempDir, nested),
        )
        assertEquals(nested.toAbsolutePath().normalize(), result)
    }

    @Test
    fun prefersNearestRootWhenMultipleRootsHaveConfig() {
        val nested = tempDir.resolve("packages/api")
        Files.createDirectories(nested)
        Files.writeString(tempDir.resolve("tspconfig.yaml"), "emit: []")
        Files.writeString(nested.resolve("tspconfig.yaml"), "emit: []")

        val result = TypeSpecContentRootResolver.findNearestTypeSpecProjectRoot(
            nested.resolve("src"),
            listOf(tempDir, nested),
        )
        assertEquals(nested.toAbsolutePath().normalize(), result)
    }

    @Test
    fun findsContentRootWhenStartIsAboveRoot() {
        Files.writeString(tempDir.resolve("tspconfig.yaml"), "emit: []")

        val result = TypeSpecContentRootResolver.findNearestTypeSpecProjectRoot(
            tempDir.resolve("..").normalize(),
            listOf(tempDir),
        )
        assertEquals(tempDir.toAbsolutePath().normalize(), result)
    }

    @Test
    fun returnsNullWhenNoRootHasConfig() {
        val other = tempDir.resolve("other")
        Files.createDirectories(other)

        assertNull(
            TypeSpecContentRootResolver.findNearestTypeSpecProjectRoot(
                other,
                listOf(tempDir, other),
            ),
        )
    }
}
