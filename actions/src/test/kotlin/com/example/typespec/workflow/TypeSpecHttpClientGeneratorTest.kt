package com.example.typespec.workflow

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class TypeSpecHttpClientGeneratorTest {
    @Test
    fun generateFromOpenApiFileCreatesRequests(@TempDir tempDir: Path) {
        Files.writeString(
            tempDir.resolve("openapi.yaml"),
            """
            paths:
              /pets:
                get: {}
              /pets/{id}:
                get: {}
            """.trimIndent(),
        )

        val content = TypeSpecHttpClientGenerator.generateFromOpenApiFile(tempDir.resolve("openapi.yaml"))
        assertTrue(content.contains("GET http://localhost:8080/pets"))
    }
}
