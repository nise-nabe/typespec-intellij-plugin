package com.example.typespec.workflow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.file.Paths

class TypeSpecTspConfigOutputDirTest {
    @Test
    fun parseOutputDirReadsTopLevelKey() {
        val yaml = """
            output-dir: "{project-root}/generated"
            emit:
              - "@typespec/openapi3"
        """.trimIndent()

        assertEquals("{project-root}/generated", TypeSpecTspConfigReader.parseOutputDir(yaml))
    }

    @Test
    fun parseOutputDirReturnsNullWhenMissing() {
        assertNull(TypeSpecTspConfigReader.parseOutputDir("emit:\n  - \"@typespec/openapi3\""))
    }

    @Test
    fun resolveOutputDirectoryDefaultsToTspOutput() {
        val root = Paths.get("project")

        assertEquals(
            root.resolve("tsp-output"),
            TypeSpecArtifactNavigator.resolveOutputDirectory(root),
        )
    }
}
