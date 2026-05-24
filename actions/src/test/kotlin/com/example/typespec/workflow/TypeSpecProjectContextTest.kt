package com.example.typespec.workflow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.nio.file.Files

class TypeSpecProjectContextTest {
    @Test
    fun findProjectRootFindsNearestTspConfig() {
        val root = Files.createTempDirectory("typespec-root-test")
        val nested = Files.createDirectories(root.resolve("nested"))
        Files.writeString(root.resolve(TypeSpecProjectContext.TSP_CONFIG_FILE_NAME), "emit:\n  - \"@typespec/openapi3\"\n")

        assertEquals(root, TypeSpecProjectContext.findProjectRoot(nested))
    }

    @Test
    fun resolveEntrypointPrefersMainTsp() {
        val root = Files.createTempDirectory("typespec-entry-test")
        Files.writeString(root.resolve("main.tsp"), "namespace Demo;")
        Files.writeString(root.resolve("other.tsp"), "namespace Other;")

        assertEquals(root.resolve("main.tsp"), TypeSpecProjectContext.resolveEntrypointFile(root, null))
    }

    @Test
    fun resolveEntrypointUsesOpenedFileWhenPresent() {
        val root = Files.createTempDirectory("typespec-opened-entry-test")
        val opened = root.resolve("service.tsp")
        Files.writeString(opened, "namespace Service;")

        assertEquals(opened, TypeSpecProjectContext.resolveEntrypointFile(root, opened))
    }
}
