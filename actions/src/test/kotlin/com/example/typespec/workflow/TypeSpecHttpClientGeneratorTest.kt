package com.example.typespec.workflow

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class TypeSpecHttpClientGeneratorTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun generatesRequestsForPathsAndMethods() {
        val openApi = tempDir.resolve("openapi.yaml")
        Files.writeString(
            openApi,
            """
            openapi: 3.0.0
            paths:
              /pets:
                get:
                  operationId: listPets
                post:
                  operationId: createPet
              /pets/{id}:
                get:
                  operationId: getPet
            """.trimIndent(),
        )

        val http = TypeSpecHttpClientGenerator.generateFromOpenApiFile(openApi)
        assertTrue(http.contains("GET http://localhost:8080/pets"))
        assertTrue(http.contains("POST http://localhost:8080/pets"))
        assertTrue(http.contains("GET http://localhost:8080/pets/{id}"))
    }

    @Test
    fun fallsBackToRootRequestWhenNoPaths() {
        val openApi = tempDir.resolve("openapi.yaml")
        Files.writeString(openApi, "openapi: 3.0.0\ninfo:\n  title: t\n")

        val http = TypeSpecHttpClientGenerator.generateFromOpenApiFile(openApi)
        assertTrue(http.contains("GET http://localhost:8080/"))
    }
}
