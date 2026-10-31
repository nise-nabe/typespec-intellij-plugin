package com.example.typespec.actions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.file.Paths

class TypeSpecFormatTargetTest {
    private val projectRoot = Paths.get("/proj")

    @Test
    fun tspConfigFormatsThatFile() {
        val config = projectRoot.resolve("tspconfig.yaml")

        assertEquals(
            "tspconfig.yaml",
            typeSpecFormatTarget("tspconfig.yaml", "yaml", projectRoot, config),
        )
    }

    @Test
    fun tspFileStaysRelativeToTheProjectRoot() {
        val file = projectRoot.resolve("src").resolve("main.tsp")

        assertEquals(
            Paths.get("src", "main.tsp").toString(),
            typeSpecFormatTarget("main.tsp", "tsp", projectRoot, file),
        )
    }

    @Test
    fun fileOutsideTheProjectRootUsesItsOwnPath() {
        val file = Paths.get("/other/main.tsp")

        assertEquals(
            file.toString(),
            typeSpecFormatTarget("main.tsp", "tsp", projectRoot, file),
        )
    }

    @Test
    fun otherFilesAreNotFormatted() {
        assertNull(
            typeSpecFormatTarget("readme.md", "md", projectRoot, projectRoot.resolve("readme.md")),
        )
    }
}
