package com.example.typespec

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class TypeSpecStandaloneTspResolverTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun buildStandaloneServerCommandLineReturnsNullWhenScriptMissing() {
        assertNull(
            TypeSpecStandaloneTspResolver.buildStandaloneServerCommandLine(
                tempDir.resolve("missing.js"),
            ) { true },
        )
    }

    @Test
    fun buildStandaloneServerCommandLineReturnsNullWhenTspUnavailable() {
        val script = tempDir.resolve("tsp-server.js")
        Files.writeString(script, "// mock server")

        assertNull(TypeSpecStandaloneTspResolver.buildStandaloneServerCommandLine(script) { false })
    }

    @Test
    fun buildStandaloneServerCommandLineIncludesServerScript() {
        val script = tempDir.resolve("tsp-server.js")
        Files.writeString(script, "// mock server")

        val commandLine = TypeSpecStandaloneTspResolver.buildStandaloneServerCommandLine(script) { true }
        assertNotNull(commandLine)
        assertTrue(commandLine!!.parametersList.parameters.contains(script.toString()))
        assertTrue(commandLine.parametersList.parameters.contains("--server"))
        assertTrue(commandLine.parametersList.parameters.contains("--stdio"))
    }
}
