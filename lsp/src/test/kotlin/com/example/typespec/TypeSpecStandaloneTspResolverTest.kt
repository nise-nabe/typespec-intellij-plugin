package com.example.typespec

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class TypeSpecStandaloneTspResolverTest {
    @Test
    fun buildStandaloneServerCommandLineReturnsNullWhenScriptMissing(@TempDir tempDir: Path) {
        assertFalse(
            TypeSpecStandaloneTspResolver.buildStandaloneServerCommandLine(
                tempDir.resolve("missing.js"),
            ) != null,
        )
    }

    @Test
    fun buildStandaloneServerCommandLineIncludesServerScript(@TempDir tempDir: Path) {
        val script = tempDir.resolve("tsp-server.js")
        Files.writeString(script, "// mock server")

        val commandLine = TypeSpecStandaloneTspResolver.buildStandaloneServerCommandLine(script)
        if (TypeSpecStandaloneTspResolver.isStandaloneTspAvailable()) {
            assertNotNull(commandLine)
            assertTrue(commandLine!!.parametersList.parameters.contains(script.toString()))
        }
    }

    private fun assertTrue(value: Boolean) = org.junit.jupiter.api.Assertions.assertTrue(value)
}
