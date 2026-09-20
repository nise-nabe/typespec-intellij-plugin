package com.example.typespec.workflow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.file.Files
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
    fun parseOutputDirHandlesInlineComment() {
        assertEquals("generated", TypeSpecTspConfigReader.parseOutputDir("output-dir: generated # build output"))
    }

    @Test
    fun parseOutputDirIgnoresNestedKeys() {
        val yaml = """
            options:
              "@typespec/openapi3":
                output-dir: nested
        """.trimIndent()

        assertNull(TypeSpecTspConfigReader.parseOutputDir(yaml))
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

    @Test
    fun resolveOutputDirectorySubstitutesProjectRootPlaceholder() {
        val root = Files.createTempDirectory("typespec-outdir-test")
        Files.writeString(root.resolve("tspconfig.yaml"), "output-dir: \"{project-root}/generated\"")

        assertEquals(
            root.resolve("generated"),
            TypeSpecArtifactNavigator.resolveOutputDirectory(root),
        )
    }

    @Test
    fun resolveOutputDirectoryUsesConfiguredRelativePath() {
        val root = Files.createTempDirectory("typespec-outdir-test")
        Files.writeString(root.resolve("tspconfig.yaml"), "output-dir: ./artifacts")

        assertEquals(
            root.resolve("artifacts"),
            TypeSpecArtifactNavigator.resolveOutputDirectory(root),
        )
    }
}
