package com.example.typespec

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class TypeSpecCompilerVersionReaderTest {
    @Test
    fun readPackageVersionReturnsVersionFromPackageJson(@TempDir tempDir: Path) {
        Files.writeString(
            tempDir.resolve("package.json"),
            """{"name":"@typespec/compiler","version":"1.13.0"}""",
        )

        assertEquals("1.13.0", TypeSpecCompilerVersionReader.readPackageVersion(tempDir))
    }

    @Test
    fun readPackageVersionReturnsNullWhenMissing(@TempDir tempDir: Path) {
        assertNull(TypeSpecCompilerVersionReader.readPackageVersion(tempDir))
    }
}
