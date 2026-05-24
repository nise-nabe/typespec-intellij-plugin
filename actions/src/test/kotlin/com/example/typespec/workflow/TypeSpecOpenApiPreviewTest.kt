package com.example.typespec.workflow

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class TypeSpecOpenApiPreviewTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun resolvePreviewEmitterRequiresOpenApi3() {
        assertEquals(
            TYPESPEC_OPENAPI3_EMITTER,
            TypeSpecOpenApiPreview.resolvePreviewEmitter(listOf("@typespec/http", TYPESPEC_OPENAPI3_EMITTER)),
        )
        assertNull(TypeSpecOpenApiPreview.resolvePreviewEmitter(listOf("@typespec/http")))
    }

    @Test
    fun findOpenApiOutputFilePrefersOpenApiJson() {
        Files.writeString(tempDir.resolve("z-spec.yaml"), "openapi: 3.0.0")
        Files.writeString(tempDir.resolve("openapi.json"), "{\"openapi\":\"3.0.0\"}")

        val selected = TypeSpecOpenApiPreview.findOpenApiOutputFile(tempDir)

        assertEquals("openapi.json", selected?.fileName?.toString())
    }

    @Test
    fun findOpenApiOutputFileIgnoresUnrecognizedJsonFiles() {
        Files.writeString(tempDir.resolve("a-spec.json"), "{\"openapi\":\"3.0.0\"}")

        assertNull(TypeSpecOpenApiPreview.findOpenApiOutputFile(tempDir))
    }

    @Test
    fun findOpenApiOutputFileAcceptsOpenApiYaml() {
        Files.writeString(tempDir.resolve("openapi.yaml"), "openapi: 3.0.0")

        val selected = TypeSpecOpenApiPreview.findOpenApiOutputFile(tempDir)

        assertEquals("openapi.yaml", selected?.fileName?.toString())
    }

    @Test
    fun buildSwaggerPreviewHtmlEmbedsSpecSafely() {
        val html = TypeSpecOpenApiPreview.buildSwaggerPreviewHtml("""{"openapi":"3.0.0"}""")

        assertTrue(html.contains("""<script type="application/json" id="typespec-openapi-spec">{"openapi":"3.0.0"}</script>"""))
        assertTrue(html.contains("JSON.parse(document.getElementById('typespec-openapi-spec').textContent)"))
    }

    @Test
    fun buildSwaggerPreviewHtmlEscapesClosingScriptTag() {
        val html = TypeSpecOpenApiPreview.buildSwaggerPreviewHtml("""{"x":"</script>"}""")

        val specBlock = html.substringAfter("typespec-openapi-spec\">").substringBefore("</script>")
        assertTrue(specBlock.contains("""<\/script>"""))
    }
}
